import { DeviceScreen, Repl } from './models';

export type Api = {
  getDeviceScreen: () => Promise<DeviceScreen>
  repl: {
    get: () => Promise<Repl>
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
    get: async (): Promise<Repl> => {
      const response = await fetch('/api/repl')
      return await response.json()
    },
    runCommand: async (yaml: string): Promise<Repl> => {
      const response = await fetch('/api/command', {
        method: 'POST',
        body: JSON.stringify({
          yaml
        })
      })
      return await response.json()
    },
    runCommandsById: async (ids: string[]): Promise<Repl> => {
      const response = await fetch('/api/command', {
        method: 'POST',
        body: JSON.stringify({
          ids
        })
      })
      return await response.json()
    },
    deleteCommands: async (ids: string[]): Promise<Repl> => {
      const response = await fetch('/api/command', {
        method: 'DELETE',
        body: JSON.stringify({
          ids
        })
      })
      return await response.json()
    }
  }
}
