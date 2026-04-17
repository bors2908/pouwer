const http = require("http");
const { URL } = require("url");

function isAuthenticated(req) {
  return String(req.headers.cookie || "").includes("browser_session=ok");
}

function sendJson(res, statusCode, body) {
  const payload = JSON.stringify(body);
  res.writeHead(statusCode, {
    "Content-Type": "application/json",
    "Content-Length": Buffer.byteLength(payload)
  });
  res.end(payload);
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url || "/", "http://localhost");

  if (req.method === "GET" && url.pathname === "/healthz") {
    res.writeHead(200, { "Content-Type": "text/plain" });
    res.end("ok");
    return;
  }

  if (!isAuthenticated(req)) {
    sendJson(res, 401, { ok: false, message: "missing or invalid session cookie" });
    return;
  }

  sendJson(res, 200, { ok: true, data: { message: "dummy API payload" } });
});

server.listen(3001, () => {
  console.log("api-app listening on 3001");
});
