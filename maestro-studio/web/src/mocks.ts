import { ResponseComposition, rest, RestContext, setupWorker } from 'msw';
import { DeviceScreen, ReplCommand } from './models';
import { sampleElements } from './sampleElements';

export const mockDeviceScreen: DeviceScreen = {
  screenshot: '/sample-screenshot.png',
  width: 1080,
  height: 2340,
  elements: sampleElements,
}

let nextId = 0
let version = 0
let commands: ReplCommand[] = []

const wait = async (durationMs: number) => {
  return new Promise(resolve => setTimeout(resolve, durationMs))
}

const replResponse = (res: ResponseComposition, ctx: RestContext) => {
  return res(ctx.status(200), ctx.json({
    version,
    commands
  }))
}

const runCommands = async (ids: string[]) => {
  for (const id of ids) {
    await runCommand(id)
  }
}

const runCommand = async (id: string) => {
  const command = commands.find(c => c.id === id)
  if (!command) return
  await wait(200)
  command.status = 'running'
  version++
  await wait(500)
  command.status = 'success'
  version++
}

const createCommand = (yaml: string) => {
  const command: ReplCommand = {
    id: `${nextId++}`,
    yaml,
    status: 'pending',
  }
  commands.push(command)
  runCommand(command.id)
  version++
}

const handlers = [
  rest.get('/api/repl/watch', async (req, res, ctx) => {
    const currentVersionParam = req.url.searchParams.get('currentVersion')
    if (currentVersionParam === null) return res(ctx.status(400))
    const requestVersion = parseInt(currentVersionParam)
    while (requestVersion >= version) {
      await wait(200)
    }
    return replResponse(res, ctx)
  }),
  rest.post('/api/repl/command', async (req, res, ctx) => {
    const {yaml, ids}: { yaml?: string, ids?: string[] } = await req.json()
    if (yaml) {
      createCommand(yaml)
    } else if (ids) {
      runCommands(ids)
    } else {
      return res(ctx.status(400))
    }
    return replResponse(res, ctx)
  }),
  rest.delete('/api/repl/command', async (req, res, ctx) => {
    const {ids}: { ids: string[] } = await req.json()
    commands = commands.filter(c => ids.includes(c.id))
    return replResponse(res, ctx)
  }),
  rest.get('/api/device-screen', (req, res, ctx) => {
    return res(ctx.delay(500), ctx.status(200), ctx.json(mockDeviceScreen))
  })
]

export const installMocks = () => {
  setupWorker(...handlers).start()
}
