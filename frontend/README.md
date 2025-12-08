# MegaBrain Frontend

Angular 20 frontend application for MegaBrain RAG Pipeline.

## Prerequisites

- Node.js 18+ and npm
- Angular CLI 20 (install globally: `npm install -g @angular/cli@20`)

## Development

### Install Dependencies

```bash
cd frontend
npm install
```

### Development Server

Start the development server with API proxy:

```bash
npm start
# or
ng serve
```

The application will be available at `http://localhost:4200` and will proxy API requests to `http://localhost:8080/api`.

### Build for Production

Build the application for production (outputs to Quarkus static resources):

```bash
npm run build
# or
ng build
```

The build output will be in `../src/main/resources/META-INF/resources` for Quarkus to serve.

## Project Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── app.component.ts      # Root component
│   │   ├── app.routes.ts          # Routing configuration
│   │   ├── app.config.ts          # Application configuration
│   │   ├── dashboard/            # Ingestion dashboard module
│   │   ├── search/               # Search interface module
│   │   └── chat/                 # RAG chat interface module
│   ├── assets/                   # Static assets
│   ├── environments/            # Environment configuration
│   ├── styles.css               # Global styles
│   ├── index.html               # Main HTML file
│   └── main.ts                  # Application entry point
├── angular.json                  # Angular CLI configuration
├── package.json                  # npm dependencies
├── tsconfig.json                 # TypeScript configuration
└── proxy.conf.json              # API proxy configuration for development
```

## Features

- **Standalone Components:** All components use Angular 20 standalone architecture (no NgModules)
- **Angular Material:** UI component library for modern design
- **RxJS:** Reactive state management
- **Prism.js:** Syntax highlighting for code preview
- **HTTP Client:** Configured for API calls to backend
- **Routing:** Lazy-loaded routes for dashboard, search, and chat modules

## API Integration

The frontend communicates with the backend API at `/api/v1`. During development, the proxy configuration (`proxy.conf.json`) forwards requests to `http://localhost:8080`.

In production, the Angular app is built and served from Quarkus static resources at `/META-INF/resources`.

## Environment Configuration

- **Development:** `src/environments/environment.ts` - API URL: `http://localhost:8080/api/v1`
- **Production:** `src/environments/environment.prod.ts` - API URL: `/api/v1` (relative)

