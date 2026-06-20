# Use Node 22 slim image
FROM node:22-slim

# Set working directory
WORKDIR /app

# Copy package files
COPY package.json package-lock.json* ./

# Install dependencies
RUN npm ci

# Copy the rest of the application files
COPY . .

# Expose the API port
EXPOSE 8787

# Define environment variables with sensible defaults
ENV PORT=8787
ENV DATABASE_PATH=/app/data/suvform.db
ENV MIGRATIONS_DIR=/app/migrations
ENV NODE_ENV=production

# Create data directory for persistent SQLite storage
RUN mkdir -p /app/data

# Run the node-entry server using tsx
CMD ["npm", "run", "start:node"]
