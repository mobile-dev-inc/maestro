import React, { createContext, ReactNode, useContext, useEffect, useState } from 'react';
import { Repl, ReplCommand, ReplCommandStatus } from '../helpers/models';
import { v4 as uuidv4 } from 'uuid';
import { API } from '../api/api';
import YAML from 'yaml';

const initialState: Repl = {
  commands: []
};

const ReplContext = createContext<{
  repl: Repl;
  setRepl: React.Dispatch<React.SetStateAction<Repl>>;
  errorMessage: string | null;
  setErrorMessage: React.Dispatch<React.SetStateAction<string | null>>;
}>({ repl: initialState, setRepl: () => {}, errorMessage: null, setErrorMessage: () => {} });

const restoreRepl = () => {
  const savedRepl = localStorage.getItem('repl');
  if (!savedRepl) return initialState
  return JSON.parse(savedRepl, (key, value) => {
    if (key === 'command' && !value) return []
    return value
  })
}

export const ReplProvider = ({ children }: {
  children: ReactNode
}) => {
  const [repl, setRepl] = useState<Repl>(() => restoreRepl());
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    localStorage.setItem('repl', JSON.stringify(repl));
  }, [repl]);

  return (
    <ReplContext.Provider value={{ repl, setRepl, errorMessage, setErrorMessage }}>
      {children}
    </ReplContext.Provider>
  );
};

export const useRepl = () => {
  const context = useContext(ReplContext);

  const { repl, setRepl, errorMessage, setErrorMessage } = context;

  const setCommandStatus = (id: string, commandStatus: ReplCommandStatus) => {
    setRepl(prevRepl => ({
      ...prevRepl,
      commands: prevRepl.commands.map(command => command.id === id ? {
        ...command,
        status: commandStatus,
      } : command),
    }));
  };

  const deleteCommands = (ids: string[]) => {
    setRepl(prevRepl => {
      const newCommands = prevRepl.commands.filter(command => !ids.includes(command.id));
      return {
        ...prevRepl,
        commands: newCommands,
      };
    });
  }

  const reorderCommands = (ids: string[]) => {
    setRepl(prevRepl => {
      const commandMap = prevRepl.commands.reduce((acc, command) => acc.set(command.id, command), new Map());
      const newCommands: ReplCommand[] = [];
      ids.forEach(id => {
        const command = commandMap.get(id);
        if (command) newCommands.push(command);
      });
      prevRepl.commands.forEach(command => {
        if (!ids.includes(command.id)) {
          newCommands.push(command);
        }
      });
      return {
        ...prevRepl,
        commands: newCommands,
      };
    });
  };

  const runCommand = async (command: ReplCommand): Promise<boolean> => {
    setCommandStatus(command.id, 'running');
    try {
      await API.runCommand(command.yaml);
      setCommandStatus(command.id, 'success');
      return true;
    } catch (e: any) {
      setCommandStatus(command.id, 'error');
      setErrorMessage(e.message || 'Failed to run command');
      return false;
    }
  };

  const runCommands = async (commands: ReplCommand[]) => {
    commands.forEach(command => setCommandStatus(command.id, 'pending'));
    let abort = false;
    for (const command of commands) {
      if (abort) {
        setCommandStatus(command.id, 'canceled');
      } else {
        const success = await runCommand(command);
        if (!success) abort = true;
      }
    }
  }

  const addCommands = (yaml: string): ReplCommand[] => {
    const parsed = YAML.parse(yaml);
    const yamls = Array.isArray(parsed) ? parsed.map(o => YAML.stringify(o)) : [YAML.stringify(parsed)];
    const commands: ReplCommand[] = yamls.map(yaml => ({
      id: `${uuidv4()}`,
      status: 'pending',
      yaml,
    }));
    setRepl(prevRepl => ({
      ...prevRepl,
      commands: [...prevRepl.commands, ...commands]
    }))
    return commands
  }

  const runCommandYaml = async (yaml: string) => {
    const commands = addCommands(yaml);
    await runCommands(commands);
  }

  const runCommandIds = async (ids: string[]) => {
    const commands = repl.commands.filter(command => ids.includes(command.id));
    await runCommands(commands);
  }

  return {
    repl,
    errorMessage,
    setErrorMessage,
    runCommandYaml,
    runCommandIds,
    deleteCommands,
    reorderCommands,
  };
};

export default ReplContext;
