# KEYSTONE Frontend Client

React + TypeScript + Vite web client for the KEYSTONE Field Service Management Platform.

---

## 🛠️ Technology Stack & Dependencies

- **Core:** React 18 & TypeScript 5
- **Build Tool:** Vite 5
- **Routing:** React Router v6 (`react-router-dom`)
- **HTTP Client:** Axios 1.7
- **Styling:** Tailwind CSS 3.4 & PostCSS
- **Icons:** Lucide React

---

## 📁 Directory Structure

```
src/
├── assets/       # Images, SVGs, static files
├── components/   # Reusable UI components
├── hooks/        # Custom React hooks
├── layouts/      # MainLayout and wrapper shells
├── pages/        # Application view pages
├── routes/       # React Router declarations (AppRoutes)
├── services/     # API services & Axios client configuration
├── types/        # TypeScript interfaces and type definitions
├── utils/        # Helper functions & formatters
├── App.tsx       # Root component wrapping BrowserRouter
├── index.css     # Global styles & Tailwind directives
└── main.tsx      # Application entry point
```

---

## ⚙️ Environment Variables

The frontend relies on Vite environment variables prefixed with `VITE_`.

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

---

## 🚀 Running the Frontend

### Install Dependencies
```bash
npm install
```

### Start Development Server
```bash
npm run dev
```

The Vite dev server will start at `http://localhost:5173`.

### Production Build
```bash
npm run build
```
The compiled static assets will be output to the `dist/` directory.

---

## Notifications

Authenticated users see a header bell (unread badge + recent dropdown) and a `/notifications` page. Both use `/api/notifications` through the shared Axios client. Recipient IDs are never sent from the browser.

`NotificationContext.applyIncomingNotification` is the hook Prompt 12 can use for WebSocket updates. This build does not poll and does not open a socket.
