import { request } from './http'
import type { ImageUploadResult } from '@/types/system'

/** A-F01 上传图片（JPG/PNG，≤256×256） */
export function uploadImage(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<ImageUploadResult>('/files/image', form)
}
