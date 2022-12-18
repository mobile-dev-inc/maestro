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
