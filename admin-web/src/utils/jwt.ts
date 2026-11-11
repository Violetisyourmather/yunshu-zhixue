/**
 * 仅解码访问令牌载荷用于界面展示与权限渲染，不做签名校验；
 * 真正的鉴权由后端完成（README 第 6 节第 8 条）。
 */
export interface AccessTokenClaims {
  sub?: string
  aud?: string
  jti?: string
  typ?: string
  perms?: string[]
  exp?: number
  /** 后端当前未写入角色声明；保留以便契约补齐后直接使用 */
  role?: string
  roleCode?: string
}

function base64UrlDecode(input: string): string {
  const base64 = input.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
  const binary = atob(padded)
  const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}

export function decodeJwtPayload(token: string): AccessTokenClaims | null {
  try {
    const payload = token.split('.')[1]
    if (!payload) {
      return null
    }
    return JSON.parse(base64UrlDecode(payload)) as AccessTokenClaims
  } catch {
    return null
  }
}
