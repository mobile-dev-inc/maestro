import { Api } from './api';
import { useState } from 'react';
import ContentEditable from 'react-contenteditable';

const PlayIcon = () => {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-6 h-6">
      <path fillRule="evenodd" d="M4.5 5.653c0-1.426 1.529-2.33 2.779-1.643l11.54 6.348c1.295.712 1.295 2.573 0 3.285L7.28 19.991c-1.25.687-2.779-.217-2.779-1.643V5.653z" clipRule="evenodd" />
    </svg>
  )
}

const ReplView = ({api}: {
  api: Api
}) => {
  const [input, setInput] = useState("")
  const {error, repl} = api.repl.useRepl()

  const runCommand = async () => {
    try {
      await api.repl.runCommand(input)
    } finally {
      setInput("")
    }
  }

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
            <pre className="font-mono">{command.yaml}</pre>
            <div>{command.status}</div>
          </div>
        ))}
        <div className="relative">
          <ContentEditable
            className="p-4 bg-gray-50 font-mono cursor-text outline-none border border-transparent focus:border focus:border-slate-400"
            html={input}
            onChange={e => setInput(e.target.value)}
            placeholder="Enter a command or interact with the device screenshot. Cmd-ENTER to run."
            onKeyDown={e => {
              if (e.code === 'Enter' && !e.shiftKey) {
                e.preventDefault()
                runCommand()
              }
            }}
          />
          <button
            className="absolute flex items-center right-1 top-1 rounded bottom-1 px-4 disabled:text-slate-400 enabled:text-blue-600 enabled:hover:bg-slate-200 enabled:active:bg-slate-300 cursor-default"
            disabled={!input}
            onClick={() => runCommand()}
          >
            <PlayIcon />
          </button>
        </div>
      </div>
    </div>
  )
}

export default ReplView
