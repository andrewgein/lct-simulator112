FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG PUBLIC_API_ENDPOINT
ARG PUBLIC_DIALOG_ENDPOINT
ENV PUBLIC_API_ENDPOINT=${PUBLIC_API_ENDPOINT}
ENV PUBLIC_DIALOG_ENDPOINT=${PUBLIC_DIALOG_ENDPOINT}
RUN npm run build

FROM node:22-alpine
WORKDIR /app
COPY --from=build /app/dist ./dist
COPY --from=build /app/node_modules ./node_modules
ENV HOST=0.0.0.0
ENV PORT=80
EXPOSE 80
ENTRYPOINT ["node", "./dist/server/entry.mjs"]
