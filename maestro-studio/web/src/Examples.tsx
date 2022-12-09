import React, { useState } from 'react';
import { UIElement } from './models';
import { CopyToClipboard } from 'react-copy-to-clipboard';

const CodeSnippet = ({children}: {
  children: string
}) => {
  const [hovered, setHovered] = useState(false)
  const [copied, setCopied] = useState(false)
  return (
    <CopyToClipboard text={children} onCopy={() => setCopied(true)}>
      <pre
        className="relative bg-slate-100 p-5 rounded overflow-x-scroll border border-transparent hover:border-slate-500 active:bg-slate-200 cursor-context-menu"
        onMouseOver={() => setHovered(true)}
        onMouseLeave={() => {
          setHovered(false)
          setCopied(false)
        }}
      >
      {children}
        {hovered ? (
          <div className="absolute top-0 right-0 flex">
            <span className="px-2 py-1 font-sans text-slate-400">{copied ? 'copied' : 'click to copy'}</span>
          </div>
        ) : null}
    </pre>
    </CopyToClipboard>
  );
}

const Section = ({ element, title, documentationUrl, codeSnippets }: {
  element: UIElement
  title: string
  documentationUrl: string
  codeSnippets: string[]
}) => {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex justify-between">
        <span className="text-slate-500">{title}</span>
        <a
          className="text-blue-400 underline underline-offset-2"
          href={documentationUrl}
          target="_blank"
          rel="noopener noreferrer"
        >View Documentation</a>
      </div>
      {codeSnippets.map(codeSnippet => {
        if (codeSnippet.includes('[id]') && !element.resourceId) return null
        if (codeSnippet.includes('[text]') && !element.text) return null
        return (
          <CodeSnippet>
            {
              codeSnippet
                .replace('[id]', element.resourceId || '')
                .replace('[text]', element.text || '')
            }
          </CodeSnippet>
        )
      })}
    </div>
  )
}

const Examples = ({ element }: {
  element: UIElement
}) => {
  const sections = template.split('===\n').map(section => {
    const paragraphs = section.split('---\n');
    const header = paragraphs.shift()!.split(',')
    const title = header[0]
    const documentationUrl = header[1]
    return (
      <Section
        element={element}
        title={title}
        documentationUrl={documentationUrl}
        codeSnippets={paragraphs}
      />
    )
  })

  return (
    <div
      className="flex flex-col gap-5 h-full overflow-y-scroll"
    >
      {element.text || element.resourceId ? (
        <>
          <div className="font-bold">
            Here are some examples of how you can interact with this element:
          </div>
          {sections}
        </>
      ) : (
        <div className="bg-red-100 rounded-md p-5">
          <p>This element does not have any <b>text</b> or <b>resource id</b> associated with it.</p>
          <br/>
          <p>The recommended approach to interact with this element is to first <b>assign it a resource id</b> in code.</p>
        </div>
      )}
    </div>
  )
}

const template = `
Tap,https://example.com
---
- tapOn: "[text]"
---
- tapOn:
    id: "[id]"
===
Assertion,https://example.com
---
- assertVisible: "[text]"
---
- assertVisible:
    id: "[id]"
===
Conditional,https://example.com
---
- runFlow:
    when:
      visible: "[text]"
      file: Subflow.yaml
---
- runFlow:
    when:
      visible:
        id: "[id]"
      file: Subflow.yaml
`

export default Examples
