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

const useRepl = (): ReplResponse => {
  const [response, setResponse] = useState<ReplResponse>({})
  useEffect(() => {
    const state = { running: true };
    ;(async () => {
      let currentVersion = -1
      while (state.running) {
        try {
          const res = await fetch(`/api/repl/watch?currentVersion=${currentVersion}`)
          const json = await res.json()
          currentVersion = json.version
          setResponse({ repl: json })
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
    const response = await fetch('/api/device-screen')
    return await response.json()
  },
  repl: {
    useRepl,
    runCommand: async (yaml: string): Promise<Repl> => {
      const response = await fetch('/api/repl/command', {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          yaml
        })
      })
      const repl: Repl = await response.json()
      await mutate('/api/repl', repl)
      return repl
    },
    runCommandsById: async (ids: string[]): Promise<Repl> => {
      const response = await fetch('/api/repl/command', {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          ids
        })
      })
      return await response.json()
    },
    deleteCommands: async (ids: string[]): Promise<Repl> => {
      const response = await fetch('/api/repl/command', {
        method: 'DELETE',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          ids
        })
      })
      return await response.json()
    }
  }
}
