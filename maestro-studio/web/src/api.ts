import { DeviceScreen, Repl } from './models';
import { mutate } from 'swr';
import { useEffect, useState } from 'react';

export type Api = {
  getDeviceScreen: () => Promise<DeviceScreen>
  repl: {
    useRepl: () => ReplResponse
    runCommand: (yaml: string) => Promise<Repl>
    runCommandsById: (ids: string[]) => Promise<Repl>
    deleteCommands: (ids: string[]) => Promise<Repl>
  }
}

export type ReplResponse = {
  repl?: Repl | undefined
  error?: any
}

const makeRequest = async <T>(method: string, path: string, body?: Object | undefined): Promise<T> => {
  const response = await fetch(path, {
    method,
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!response.ok) {
    const body = await response.text()
    throw new Error(`Request failed ${response.status}: ${body}`)
  }
  return await response.json()
}

const useRepl = (): ReplResponse => {
  const [response, setResponse] = useState<ReplResponse>({})
  useEffect(() => {
    const state = { running: true };
    ;(async () => {
      let currentVersion = -1
      while (state.running) {
        try {
          const repl: Repl = await makeRequest('GET', `/api/repl/watch?currentVersion=${currentVersion}`)
          currentVersion = repl.version
          setResponse({ repl })
        } catch (e) {
          setResponse({ error: e })
          await new Promise(resolve => setTimeout(resolve, 1000))
        }
      }
    })()
    return () => { state.running = false }
  }, [])
  return response
}

export const REAL_API: Api = {
  getDeviceScreen: async (): Promise<DeviceScreen> => {
    return makeRequest('GET', '/api/device-screen')
  },
  repl: {
    useRepl,
    runCommand: async (yaml: string): Promise<Repl> => {
      const repl: Repl = await makeRequest('POST', '/api/repl/command', { yaml })
      await mutate('/api/repl', repl)
      return repl
    },
    runCommandsById: async (ids: string[]): Promise<Repl> => {
      return makeRequest('POST', '/api/repl/command', { ids })
    },
    deleteCommands: async (ids: string[]): Promise<Repl> => {
      return makeRequest('DELETE', '/api/repl/command', { ids })
    }
  }
}
