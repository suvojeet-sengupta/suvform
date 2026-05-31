/**
 * Safe arithmetic evaluator + calculation recompute for the Worker.
 *
 * This is the server-side mirror of the recursive-descent evaluator used in the
 * public form filler (see publicForm.ts `evalExpr`) and the Android app
 * (ExpressionEvaluator.kt). It is the authoritative computation: response
 * calculations are recomputed here from the stored answers so a tampered client
 * payload cannot poison `calculated_json`.
 *
 * Supports: + - * / % ( ), numbers, and field-id variables. No eval / Function —
 * fully sandboxed and deterministic. Unknown ids resolve to 0; division/modulo
 * by zero yields 0 (matching the client behaviour).
 */

type Token =
  | { t: "num"; v: number }
  | { t: "id"; v: string }
  | { t: "op"; v: string }
  | { t: "end" };

function tokenize(s: string): Token[] {
  const out: Token[] = [];
  let i = 0;
  while (i < s.length) {
    const c = s[i];
    if (/\s/.test(c)) {
      i++;
      continue;
    }
    if (/[0-9.]/.test(c)) {
      let j = i;
      while (j < s.length && /[0-9.]/.test(s[j])) j++;
      out.push({ t: "num", v: parseFloat(s.slice(i, j)) });
      i = j;
      continue;
    }
    if (/[a-zA-Z_]/.test(c)) {
      let j = i;
      while (j < s.length && /[a-zA-Z0-9_]/.test(s[j])) j++;
      out.push({ t: "id", v: s.slice(i, j) });
      i = j;
      continue;
    }
    if ("+-*/%()".indexOf(c) >= 0) {
      out.push({ t: "op", v: c });
      i++;
      continue;
    }
    throw new Error("bad char " + c);
  }
  out.push({ t: "end" });
  return out;
}

export function evalExpr(expr: string, vars: Record<string, number>): number {
  const tk = tokenize(expr);
  let pos = 0;
  const eat = () => tk[pos++];
  // Returns the operator symbol at the cursor, or null if it isn't an operator.
  // Reading through this helper keeps TS narrowing happy across loop iterations.
  const peekOp = (): string | null => {
    const t = tk[pos];
    return t.t === "op" ? t.v : null;
  };

  function expr_(): number {
    let l = term();
    let op = peekOp();
    while (op === "+" || op === "-") {
      eat();
      const r = term();
      l = op === "+" ? l + r : l - r;
      op = peekOp();
    }
    return l;
  }
  function term(): number {
    let l = unary();
    let op = peekOp();
    while (op === "*" || op === "/" || op === "%") {
      eat();
      const r = unary();
      if ((op === "/" || op === "%") && r === 0) l = 0;
      else l = op === "*" ? l * r : op === "/" ? l / r : l % r;
      op = peekOp();
    }
    return l;
  }
  function unary(): number {
    const op = peekOp();
    if (op === "-") {
      eat();
      return -unary();
    }
    if (op === "+") {
      eat();
      return unary();
    }
    return primary();
  }
  function primary(): number {
    const t = eat();
    if (t.t === "num") return t.v;
    if (t.t === "id") return Number(vars[t.v]) || 0;
    if (t.t === "op" && t.v === "(") {
      const v = expr_();
      const close = eat();
      if (close.t !== "op" || close.v !== ")") throw new Error("expected )");
      return v;
    }
    throw new Error("unexpected");
  }

  const result = expr_();
  return Number.isFinite(result) ? result : 0;
}

type FieldLike = { id?: string; type?: string };
type CalcLike = { id?: string; expression?: string };

/**
 * Authoritatively recompute every calculation from validated answers.
 * Mirrors the client: only `number` and `rating` fields feed the variables;
 * all other field types resolve to 0.
 */
export function recomputeCalculations(
  fields: FieldLike[],
  calculations: CalcLike[],
  answers: Record<string, unknown>,
): Record<string, number> {
  const vars: Record<string, number> = {};
  for (const f of fields) {
    if (!f || typeof f.id !== "string") continue;
    const v = answers[f.id];
    if (f.type === "number") vars[f.id] = parseFloat(String(v)) || 0;
    else if (f.type === "rating") vars[f.id] = Number(v) || 0;
    else vars[f.id] = 0;
  }

  const out: Record<string, number> = {};
  for (const c of calculations) {
    if (!c || typeof c.id !== "string" || typeof c.expression !== "string") continue;
    try {
      out[c.id] = evalExpr(c.expression, vars);
    } catch {
      out[c.id] = 0;
    }
  }
  return out;
}
