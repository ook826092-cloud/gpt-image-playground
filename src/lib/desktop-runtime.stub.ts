// Stub for non-desktop builds (Android/Web) — replaces @/lib/desktop-runtime
// All functions return safe defaults indicating "not a desktop environment".

import type { MouseEventHandler } from 'react';

export type DesktopUpdateDownloadEvent =
    | { event: 'Started'; data: { contentLength?: number } }
    | { event: 'Progress'; data: { chunkLength: number } }
    | { event: 'Finished' };

export type DesktopUpdate = {
    currentVersion: string;
    version: string;
    date?: string;
    body?: string;
    downloadAndInstall: (onEvent?: (event: DesktopUpdateDownloadEvent) => void) => Promise<void>;
};

export function isTauriDesktop(): boolean {
    return false;
}

export async function copyTextToClipboard(_text: string): Promise<boolean> {
    return false;
}

export async function readDesktopClipboardImageFile(_filename?: string): Promise<File | null> {
    return null;
}

export async function openExternalUrl(url: string): Promise<void> {
    window.open(url, '_blank', 'noopener,noreferrer');
}

export function handleExternalLinkClick(url: string): MouseEventHandler<HTMLAnchorElement> {
    return (e) => {
        e.preventDefault();
        window.open(url, '_blank', 'noopener,noreferrer');
    };
}

export async function invokeDesktopCommand<T>(_command: string, _args?: Record<string, unknown>): Promise<T> {
    throw new Error('Desktop commands are not available on this platform');
}

export async function invokeDesktopStreamingCommand<T>(
    _command: string,
    _args?: Record<string, unknown>,
): Promise<AsyncIterable<T>> {
    throw new Error('Desktop streaming commands are not available on this platform');
}

export async function checkDesktopUpdate(): Promise<DesktopUpdate | null> {
    return null;
}

export async function installDesktopUpdate(
    _update: DesktopUpdate,
    _onEvent?: (event: DesktopUpdateDownloadEvent) => void,
): Promise<void> {
    throw new Error('Desktop updates are not available on this platform');
}

export async function relaunchDesktopApp(): Promise<void> {
    throw new Error('Desktop relaunch is not available on this platform');
}
