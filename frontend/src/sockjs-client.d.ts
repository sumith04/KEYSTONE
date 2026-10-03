declare module 'sockjs-client' {
  export default class SockJS {
    constructor(url: string, protocols?: string | string[] | null, options?: object);
    close(code?: number, reason?: string): void;
    send(data: string): void;
    onopen: ((event: Event) => void) | null;
    onclose: ((event: CloseEvent) => void) | null;
    onmessage: ((event: MessageEvent) => void) | null;
    onerror: ((event: Event) => void) | null;
    readyState: number;
  }
}
