const http = require("http");
const { URL } = require("url");

function hasSession(req) {
  return String(req.headers.cookie || "").includes("browser_session=ok");
}

function redirectHome(res, extraHeaders = {}) {
  res.writeHead(303, {
    Location: "/",
    ...extraHeaders
  });
  res.end();
}

function renderHome(isLoggedIn) {
  const actionForm = isLoggedIn
    ? `
      <form method="post" action="/logout">
        <button type="submit">Logout</button>
      </form>
    `
    : `
      <form method="post" action="/login">
        <button type="submit">Login</button>
      </form>
    `;

  return `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Browser app</title>
    <style>
      body { font-family: Arial, sans-serif; max-width: 640px; margin: 2rem auto; }
      .card { border: 1px solid #ddd; border-radius: 8px; padding: 1rem; }
      button { padding: 0.5rem 0.75rem; }
    </style>
  </head>
  <body>
    <h1>Browser app</h1>
    <div class="card">
      <p>Session status: <strong>${isLoggedIn ? "logged in" : "logged out"}</strong></p>
      <p>Trigger the browser captcha by refreshing the page too fast or by clicking login/logout repeatedly.</p>
      ${actionForm}
    </div>
  </body>
</html>`;
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url || "/", "http://localhost");

  if (req.method === "GET" && url.pathname === "/healthz") {
    res.writeHead(200, { "Content-Type": "text/plain" });
    res.end("ok");
    return;
  }

  if (req.method === "POST" && url.pathname === "/login") {
    redirectHome(res, {
      "Set-Cookie": "browser_session=ok; Path=/; HttpOnly; SameSite=Lax"
    });
    return;
  }

  if (req.method === "POST" && url.pathname === "/logout") {
    redirectHome(res, {
      "Set-Cookie": "browser_session=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0"
    });
    return;
  }

  if (req.method === "GET" && url.pathname === "/") {
    const body = renderHome(hasSession(req));
    res.writeHead(200, {
      "Content-Type": "text/html; charset=utf-8",
      "Content-Length": Buffer.byteLength(body)
    });
    res.end(body);
    return;
  }

  res.writeHead(404, { "Content-Type": "text/plain" });
  res.end("not found");
});

server.listen(3000, () => {
  console.log("browser-app listening on 3000");
});
