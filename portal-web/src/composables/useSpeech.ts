import { onScopeDispose, ref } from 'vue'

/**
 * 浏览器语音识别封装（Q4 / T3）：Web Speech API，Chrome / Edge 支持；
 * 不支持的浏览器由调用方降级为键盘输入，不做服务端语音转写。
 */
interface SpeechRecognitionResultLike {
  isFinal: boolean
  0: { transcript: string }
}

interface SpeechRecognitionEventLike {
  resultIndex: number
  results: ArrayLike<SpeechRecognitionResultLike> & {
    [index: number]: SpeechRecognitionResultLike
  }
}

interface SpeechRecognitionLike {
  continuous: boolean
  interimResults: boolean
  lang: string
  start(): void
  stop(): void
  onresult: ((event: SpeechRecognitionEventLike) => void) | null
  onerror: ((event: { error: string }) => void) | null
  onend: (() => void) | null
}

type SpeechRecognitionCtor = new () => SpeechRecognitionLike

function getRecognitionCtor(): SpeechRecognitionCtor | null {
  const scope = window as unknown as {
    SpeechRecognition?: SpeechRecognitionCtor
    webkitSpeechRecognition?: SpeechRecognitionCtor
  }
  return scope.SpeechRecognition ?? scope.webkitSpeechRecognition ?? null
}

export function useSpeech() {
  const supported = Boolean(getRecognitionCtor())
  const listening = ref(false)
  const transcript = ref('')
  const error = ref('')

  let recognition: SpeechRecognitionLike | null = null

  function start(): void {
    const Ctor = getRecognitionCtor()
    if (!Ctor) {
      error.value = '当前浏览器不支持语音识别'
      return
    }
    error.value = ''
    transcript.value = ''
    recognition ??= createRecognition(Ctor)
    recognition.start()
    listening.value = true
  }

  function createRecognition(Ctor: SpeechRecognitionCtor): SpeechRecognitionLike {
    const instance = new Ctor()
    instance.continuous = true
    instance.interimResults = true
    instance.lang = 'zh-CN'
    instance.onresult = (event) => {
      let text = ''
      for (let index = event.resultIndex; index < event.results.length; index += 1) {
        text += event.results[index][0].transcript
      }
      transcript.value = text.trim()
    }
    instance.onerror = (event) => {
      error.value = `语音识别失败：${event.error}`
      listening.value = false
    }
    instance.onend = () => {
      listening.value = false
    }
    return instance
  }

  function stop(): void {
    recognition?.stop()
    listening.value = false
  }

  function reset(): void {
    transcript.value = ''
    error.value = ''
  }

  onScopeDispose(stop)

  return { supported, listening, transcript, error, start, stop, reset }
}
