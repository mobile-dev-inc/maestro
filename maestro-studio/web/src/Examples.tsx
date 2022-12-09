import React from 'react';
import { UIElement } from './models';

const Section = ({ title, documentationUrl, codeSnippets }: {
  title: string
  documentationUrl: string
  codeSnippets: string[]
}) => {
  return (
    <div className="flex flex-col">
      <div className="flex justify-between">
        <span>{title}</span>
        <a href={documentationUrl}></a>
      </div>
    </div>
  )
}

const Examples = ({ element }: {
  element: UIElement
}) => {
  return (
    <div
      className="font-bold flex flex-col"
    >
      <div>
        Here are some examples of how you can interact with this element:
      </div>
      <Section
        title="Tap"
        documentationUrl={""}
        codeSnippets={[]}
      />
    </div>
  )
}

export default Examples
