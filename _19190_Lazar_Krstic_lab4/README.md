# Lab 4 - Web Search Application

This project is a small web application for indexing and searching the 400 split text documents from the first laboratory exercise.

The backend is implemented with .NET Web API, the frontend is implemented with React and TypeScript, and Elasticsearch is used as the search engine.

## Requirements

- .NET SDK 8
- Node.js LTS and npm
- Docker Desktop
- Elasticsearch started through Docker Compose

## Quick Start

From the project folder:

```bash
scripts\init_env.cmd
```

Before indexing, place the generated `.txt` document parts in:

```text
data/documents
```

Start Elasticsearch:

```bash
docker compose -f scripts\docker-compose.yml up -d
```

Start the backend and frontend:

```bash
scripts\start_app.cmd
```

The script opens two terminal windows:

- backend: `http://localhost:5044`
- frontend: `http://localhost:5173`

## Manual Run

Start Elasticsearch:

```bash
docker compose -f scripts\docker-compose.yml up -d
```

Run the backend:

```bash
cd backend
dotnet run
```

Run the frontend in a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the application:

```text
http://localhost:5173
```

## Expected Result

In the web application:

1. Click `Indeksiraj`.
2. Wait until the application reports 400 indexed documents.
3. Select a search field.
4. Enter a query such as `ghost`, `wallpaper`, or `vampyre`.
5. Click `Trazi`.
6. Results should be shown with 5 documents per page.
7. Each result should include score, file name, metadata, preview text, and a download link.

## Main API Endpoints

- `GET /api/health`
- `GET /api/fields`
- `GET /api/index/status`
- `POST /api/index/rebuild`
- `POST /api/search`
- `GET /api/documents/{id}/download`

## Notes

Docker is used only to run Elasticsearch locally. The original data are stored locally as `.txt` files in `data/documents`, while Elasticsearch stores the searchable index. The document files are ignored in Git because they are generated input data.
