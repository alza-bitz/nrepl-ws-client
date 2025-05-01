# nREPL WebSocket Client

A method for using a browser to write, load and evaluate Clojure code connected to a [remote nREPL server using websockets](), processing the results for display using the Scicloj notebook library [clay](https://scicloj.github.io/clay).

## Features

- ClojureScript app using [Reagent](https://github.com/reagent-project/reagent)
- Rich code editing with syntax highlighting courtesy of [CodeMirror](https://codemirror.net) and [clojure-mode](https://nextjournal.github.io/clojure-mode)
- Multiple evaluation modes :
  - REPL mode: Standard evaluation with text output
  - [Clay](https://scicloj.github.io/clay) mode: Notebook evaluation using indirect rendering inside an iframe
  - [Clay Hiccup](https://scicloj.github.io/clay/#hiccup-output) mode: As above but using direct rendering of [Hiccup](https://github.com/weavejester/hiccup) data in the Reagent interface 
- Keyboard shortcuts for evaluation:
  - `Alt+Enter`: Evaluate editor content
  - `Ctrl+Enter`: Evaluate form at cursor
  - `Ctrl+Shift+Enter`: Evaluate top-level form at cursor

## Prerequisites

- Node.js and npm
- Java 11 or later (tested with Java 21)
- [Clojure CLI tools](https://clojure.org/guides/install_clojure)
- [nREPL server with WebSocket support](https://github.com/alza-bitz/nrepl-ws-server) running at `ws://localhost:7888`.

Alternatively, use an editor or environment that supports dev containers. The supplied [devcontainer.json](.devcontainer/devcontainer.json) will install all the above prerequisites.

## Usage

Clone the server repository :
```bash
cd /workspaces
git clone https://github.com/alza-bitz/nrepl-ws-server.git
```

Start the Websocket nREPL server in a separate terminal :
```
cd /workspaces/nrepl-ws-server
clojure -M:nrepl-ws
```

Clone the client repository :
```bash
cd /workspaces
git clone https://github.com/alza-bitz/nrepl-ws-client.git
cd nrepl-ws-client
```

Install the dependencies :
```bash
npm install
```

Start the [shadow-cljs](https://github.com/thheller/shadow-cljs) server :
```bash
clojure -M:shadow-cljs watch main
```
Alternatively, use the built-in support for Shadow CLJS provided by your editor.

Finally, open your browser at [http://localhost:8020](http://localhost:8020)

## Development

### Start the shadow-cljs server
```
clojure -M:shadow-cljs watch main
```

### Running the Tests
```
clojure -M:shadow-cljs:test compile test
```

### Build for Production
```
clojure -M:shadow-cljs release main`
```

## License

Copyright © 2025 Alex Coyle

Distributed under the Eclipse Public License version 2.0.

See the [LICENSE](LICENSE) file for details.
