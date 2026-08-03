export function createSessionStore() {
  let session = null;

  return {
    get() {
      return session;
    },
    set(nextSession) {
      session = nextSession ? Object.freeze({ ...nextSession }) : null;
      return session;
    },
    clear() {
      session = null;
    },
  };
}
