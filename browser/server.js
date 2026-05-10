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
        <button class="btn btn-ghost" type="submit">Logout</button>
      </form>
    `
    : `
      <form method="post" action="/login">
        <button class="btn btn-primary" type="submit">Login</button>
      </form>
    `;

  const identityCard = isLoggedIn
    ? `
      <div class="identity-row">
        <div class="avatar">N</div>
        <div>
          <strong>nothing.user</strong>
          <div class="presence"><span class="dot"></span>Presently online</div>
        </div>
      </div>
    `
    : `
      <div class="identity-row">
        <div class="avatar avatar-muted">?</div>
        <div>
          <strong>anonymous void</strong>
          <div class="presence presence-muted"><span class="dot dot-muted"></span>Not logged in</div>
        </div>
      </div>
    `;

  const probeForms = `
      <form method="post" action="/login?noop=1">
        <button class="btn btn-probe" type="submit">Become Void</button>
      </form>
      <form method="post" action="/logout?noop=1">
        <button class="btn btn-probe" type="submit">Become Nothing</button>
      </form>
      <form method="post" action="/login?noop=1&source=void">
        <button class="btn btn-probe" type="submit">Become Empty</button>
      </form>
  `;

  return `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Nothing, Inc.</title>
    <style>
      :root {
        --bg: #0f1115;
        --bg-soft: #171a21;
        --text: #e7eaf2;
        --muted: #97a0b7;
        --line: #2a3040;
        --accent: #6f8cff;
        --accent-2: #14b86a;
      }
      * { box-sizing: border-box; }
      body {
        margin: 0;
        font-family: Inter, Segoe UI, Roboto, Arial, sans-serif;
        color: var(--text);
        background:
          radial-gradient(circle at 20% 0%, #20273a 0, transparent 40%),
          radial-gradient(circle at 80% 0%, #18203a 0, transparent 35%),
          var(--bg);
      }
      .shell {
        max-width: 960px;
        margin: 0 auto;
        padding: 2.5rem 1rem 3rem;
      }
      .hero {
        border: 1px solid var(--line);
        border-radius: 16px;
        padding: 1.5rem;
        background: linear-gradient(180deg, rgba(255,255,255,0.04), rgba(255,255,255,0.01));
      }
      .session-header {
        margin-top: 1rem;
        border: 1px solid var(--line);
        border-radius: 16px;
        padding: 1rem;
        background: var(--bg-soft);
      }
      .chip {
        display: inline-block;
        border: 1px solid var(--line);
        border-radius: 999px;
        padding: 0.3rem 0.65rem;
        color: var(--muted);
        font-size: 0.85rem;
      }
      h1 {
        margin: 0.7rem 0 0.5rem;
        font-size: 2rem;
      }
      .lead {
        margin: 0;
        color: var(--muted);
        line-height: 1.6;
      }
      .grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
        gap: 1rem;
        margin-top: 1rem;
      }
      .card {
        border: 1px solid var(--line);
        border-radius: 16px;
        padding: 1rem;
        background: var(--bg-soft);
      }
      .identity-row {
        display: flex;
        align-items: center;
        gap: 0.85rem;
        margin-bottom: 1rem;
      }
      .avatar {
        width: 46px;
        height: 46px;
        border-radius: 50%;
        display: grid;
        place-items: center;
        font-weight: 700;
        color: #fff;
        background: linear-gradient(135deg, var(--accent), #8d6dff);
      }
      .avatar-muted { background: #3f465c; }
      .presence {
        margin-top: 0.2rem;
        color: #b9f3d6;
        display: flex;
        align-items: center;
        gap: 0.45rem;
        font-size: 0.92rem;
      }
      .presence-muted { color: var(--muted); }
      .dot {
        width: 9px;
        height: 9px;
        border-radius: 50%;
        background: var(--accent-2);
        box-shadow: 0 0 10px rgba(20, 184, 106, 0.7);
      }
      .dot-muted { background: #7c8398; box-shadow: none; }
      .actions { display: flex; flex-wrap: wrap; gap: 0.5rem; }
      .actions form { margin: 0; }
      .btn {
        border: 1px solid var(--line);
        border-radius: 10px;
        padding: 0.55rem 0.85rem;
        font-weight: 600;
        color: var(--text);
        background: #232838;
        cursor: pointer;
      }
      .btn:hover { filter: brightness(1.08); }
      .btn-primary { background: linear-gradient(135deg, #4463ff, #7d4dff); border-color: transparent; }
      .btn-ghost { background: #252b3a; }
      .btn-probe { background: #1b2231; color: #b8c1d8; }
      h2 {
        margin: 0 0 0.55rem;
        font-size: 1.05rem;
      }
      p {
        margin: 0;
        color: var(--muted);
        line-height: 1.65;
      }
      .stack { display: grid; gap: 0.85rem; }
      .small { font-size: 0.85rem; color: #7f88a0; margin-top: 0.7rem; }
    </style>
  </head>
  <body>
    <div class="shell">
      <section class="hero">
        <span class="chip">The Department of Absolutely Nothing</span>
        <h1>Nothing, fully explained.</h1>
        <p class="lead">A modern destination for people who believe that emptiness is not a bug, but a feature. Here we study the shape of absence, the texture of silence, and the productivity of doing exactly nothing.</p>
      </section>

      <header class="session-header stack">
        <h2>Identity</h2>
        ${identityCard}
        <div class="actions">
          ${actionForm}
          ${probeForms}
        </div>
      </header>

      <div class="grid">
        <article class="card stack">
          <h2>What Is Nothing?</h2>
          <p>Nothing is the room where all ideas wait before they become something. It is the pause between two notes, the blank margin around a poem, and the calm that lets meaning breathe.</p>
          <p>Philosophers have chased nothing for centuries. Some found freedom, some found anxiety, and some found that naming nothing already turns it into something.</p>
          <p>Our official position: nothing is useful. It gives contrast to everything else.</p>
        </article>

        <article class="card stack">
          <h2>Daily Practices of Nothingness</h2>
          <p>1) Stare at a wall for 30 seconds. 2) Close one unnecessary tab. 3) Let one thought pass without fixing it. Repeat until serenity or snack time.</p>
          <p>Experts agree that intentional emptiness improves attention. Even machines appreciate small quiet windows between requests.</p>
        </article>

        <article class="card stack">
          <h2>Frequently Unasked Questions</h2>
          <p><strong>Q:</strong> Is there content here?<br/><strong>A:</strong> Barely, and that's the point.</p>
          <p><strong>Q:</strong> What happens if I click probe buttons repeatedly?<br/><strong>A:</strong> The app does almost nothing, while your edge layer may become deeply interested.</p>
        </article>
      </div>
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
    if (url.searchParams.get("noop") === "1") {
      redirectHome(res);
      return;
    }

    redirectHome(res, {
      "Set-Cookie": "browser_session=ok; Path=/; HttpOnly; SameSite=Lax"
    });
    return;
  }

  if (req.method === "POST" && url.pathname === "/logout") {
    if (url.searchParams.get("noop") === "1") {
      redirectHome(res);
      return;
    }

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

server.listen(3002, () => {
  console.log("browser-app listening on 3002");
});
