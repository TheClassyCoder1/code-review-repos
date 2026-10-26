# Image for the statement-export API (customer profile + KYC routes included).
FROM node:20.15-bookworm-slim AS deps

WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci --omit=dev --no-audit --no-fund

FROM node:20.15-bookworm-slim

ENV NODE_ENV=production \
    PORT=8101

WORKDIR /srv/statement-export
COPY --from=deps /app/node_modules ./node_modules
COPY package.json ./
COPY src ./src

RUN mkdir -p /var/lib/statements/rendered /var/lib/statements/documents

EXPOSE 8101

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD node -e "require('http').get('http://127.0.0.1:8101/api/v1/health', r => process.exit(r.statusCode === 200 ? 0 : 1)).on('error', () => process.exit(1))"

CMD ["node", "src/server.js"]
