# The frontend is prebuilt into ./dist (see README: `cd web && npm run build`).
# This keeps the runtime image small and avoids running npm inside Docker.
FROM nginx:1.28-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist /usr/share/nginx/html
EXPOSE 80
