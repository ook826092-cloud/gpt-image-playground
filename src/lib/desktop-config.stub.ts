// Stub for non-desktop builds (Android/Web) — replaces @/lib/desktop-config

export type DesktopProxyMode = 'disabled' | 'system' | 'manual';
export type DesktopPromoServiceMode = 'disabled' | 'current' | 'origin' | 'endpoint';

export type DesktopProxyConfig = { mode: 'disabled' } | { mode: 'system' } | { mode: 'manual'; url: string };

export type DesktopPromoServiceConfig =
    | { mode: 'disabled'; placementsUrl: null }
    | { mode: 'current'; placementsUrl: string }
    | { mode: 'origin' | 'endpoint'; url: string; placementsUrl: string };

export type DesktopPublicRuntimeConfig =
    | { mode: 'disabled'; configUrl: null }
    | { mode: 'current'; configUrl: string }
    | { mode: 'origin' | 'endpoint'; url: string; configUrl: string };

export function buildDesktopProxyConfig(_proxyMode: DesktopProxyMode, _proxyUrl: string): DesktopProxyConfig {
    return { mode: 'disabled' };
}

export function normalizeDesktopProxyUrl(_url: string): string | null {
    return null;
}

export function isValidProxyUrl(_url: string): boolean {
    return false;
}

export function normalizeDesktopProxyMode(_mode: string): DesktopProxyMode {
    return 'disabled';
}

export function normalizeDesktopPromoServiceMode(_mode: string): DesktopPromoServiceMode {
    return 'disabled';
}

export function normalizeDesktopPromoServiceUrl(_url: string): string {
    return '';
}

export function buildDesktopPromoPlacementsUrl(_config: DesktopPromoServiceConfig): string | null {
    return null;
}

export function buildDesktopPublicRuntimeConfigUrl(_config: DesktopPublicRuntimeConfig): string | null {
    return null;
}

export function compareSemver(a: string, b: string): number {
    const pa = a.split('.').map(Number);
    const pb = b.split('.').map(Number);
    for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
        const va = pa[i] || 0;
        const vb = pb[i] || 0;
        if (va > vb) return 1;
        if (va < vb) return -1;
    }
    return 0;
}

export function isNewerVersion(current: string, latest: string): boolean {
    return compareSemver(latest, current) > 0;
}

export function desktopProxyConfigFromAppConfig(_config: unknown): DesktopProxyConfig {
    return { mode: 'disabled' };
}

export function desktopPromoServiceConfigFromAppConfig(_config: unknown): DesktopPromoServiceConfig {
    return { mode: 'disabled', placementsUrl: null };
}

export function desktopPublicRuntimeConfigFromAppConfig(_config: unknown): DesktopPublicRuntimeConfig {
    return { mode: 'disabled', configUrl: null };
}

export const DESKTOP_APP_DOWNLOAD_URL = '';
export const DESKTOP_APP_GUIDANCE_TITLE = '';
export const DESKTOP_APP_GUIDANCE_MESSAGE = '';
export const DESKTOP_ONLY_SETTINGS_MESSAGE = '';

export function appendDesktopAppGuidance(_errors: string[]): string[] {
    return _errors;
}

export function isLikelyWebDirectAccessError(_error: unknown): boolean {
    return false;
}
