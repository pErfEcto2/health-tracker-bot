export type Route =
  | "login"
  | "recover"
  | "change-password"
  | "setup"
  | "home"
  | "food"
  | "workout"
  | "journal"
  | "profile";

export const Routes: Route[] = [
  "login", "recover", "change-password", "setup",
  "home", "food", "workout", "journal", "profile",
];

export type RouteHandler = () => void | Promise<void>;
const handlers: Partial<Record<Route, RouteHandler>> = {};

export function registerRoute(route: Route, handler: RouteHandler): void {
  handlers[route] = handler;
}

export function routeHandler(route: Route): RouteHandler | undefined {
  return handlers[route];
}

export function currentRoute(): Route {
  const hash = window.location.hash.replace(/^#\/?/, "");
  const r = hash.split("?")[0] as Route;
  return (Routes.includes(r) ? r : "home");
}

export function navigate(route: Route): void {
  const target = `#/${route}`;
  if (window.location.hash !== target) {
    window.location.hash = target;
  } else {
    // already there — force re-render
    void handlers[route]?.();
  }
}

export function start(): void {
  window.addEventListener("hashchange", () => {
    void handlers[currentRoute()]?.();
  });
  void handlers[currentRoute()]?.();
}
