const AUTH_SESSION_ENDED_EVENT = 'auth:session-ended';

export function emitAuthSessionEnded() {
  if (typeof window === 'undefined') {
    return;
  }

  window.dispatchEvent(new Event(AUTH_SESSION_ENDED_EVENT));
}

export function onAuthSessionEnded(handler: () => void) {
  if (typeof window === 'undefined') {
    return () => {};
  }

  window.addEventListener(AUTH_SESSION_ENDED_EVENT, handler);
  return () => window.removeEventListener(AUTH_SESSION_ENDED_EVENT, handler);
}
