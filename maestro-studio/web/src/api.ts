import { DeviceScreen, Repl } from './models';
import useSWR, { mutate, SWRConfiguration, SWRResponse } from 'swr';

const fetcher = async <JSON = any>(input: RequestInfo, init?: RequestInit): Promise<JSON> => {
  const res = await fetch(input, init)
  return res.json()
}

export type Api = {
  getDeviceScreen: () => Promise<DeviceScreen>
  repl: {
    useRepl: (config?: SWRConfiguration) => SWRResponse<Repl>
    runCommand: (yaml: string) => Promise<Repl>
    runCommandsById: (ids: string[]) => Promise<Repl>
    deleteCommands: (ids: string[]) => Promise<Repl>
  }
}

export const REAL_API: Api = {
  getDeviceScreen: async (): Promise<DeviceScreen> => {
    const response = await fetch('/api/device-screen')
    return await response.json()
  },
  repl: {
    useRepl: (config?: SWRConfiguration): SWRResponse<Repl> => {
      return useSWR<Repl>('/api/repl', fetcher, config)
    },
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
