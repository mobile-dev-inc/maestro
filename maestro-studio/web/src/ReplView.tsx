import { Api } from './api';
import { useState } from 'react';

const ReplView = ({api}: {
  api: Api
}) => {
  const [input, setInput] = useState("")
  const {error, data} = api.repl.useRepl({
    refreshInterval: 1000
  })

  if (error) {
    return (
      <div>Error fetching repl</div>
    )
  }

  if (!data) {
    return (
      <div>Loading...</div>
    )
  }

  return (
    <div>
      <div className="flex flex-col">
        {data.commands.map(command => (
          <div className="flex flex-row border">
            <div>{command.yaml}</div>
            <div>{command.status}</div>
          </div>
        ))}
      </div>
      <textarea value={input} onChange={e => setInput(e.target.value)}/>
      <button onClick={() => api.repl.runCommand(input)}>Send</button>
    </div>
  )
}

export default ReplView
