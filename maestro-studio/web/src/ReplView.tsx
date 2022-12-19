import { Api } from './api';
import { useState } from 'react';

const ReplView = ({api}: {
  api: Api
}) => {
  const [input, setInput] = useState("")
  const {error, repl} = api.repl.useRepl()

  if (error) {
    return (
      <div>Error fetching repl</div>
    )
  }

  if (!repl) {
    return (
      <div>Loading...</div>
    )
  }

  return (
    <div>
      <div className="flex flex-col border">
        {repl.commands.map(command => (
          <div className="flex flex-row p-4 border-b justify-between">
            <div className="font-mono">{command.yaml}</div>
            <div>{command.status}</div>
          </div>
        ))}
        <textarea
          className="bg-gray-50 font-mono p-2"
          placeholder="Enter a command or interact with the device screenshot"
          value={input}
          onChange={e => setInput(e.target.value)}
        />
        <button
          className="bg-blue-700 text-white"
          onClick={() => {
            (async () => {
              try {
                await api.repl.runCommand(input)
              } finally {
                setInput("")
              }
            })()
          }}
        >
          Send
        </button>
      </div>
    </div>
  )
}

export default ReplView
