import Editor from "@monaco-editor/react";
import { useEffect, useState } from "react";

const EditorView = () => {
  const [code, setCode] = useState("");
  const [file, setFile] = useState();

  const handleFileChange = (event: any) => {
    if (event.target.files) {
      setFile(event.target.files[0]);
      console.log(event.target.files[0]);
    }
  };

  useEffect(() => {
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        if (e.target !== null) {
          if (e.target.result !== null) {
            setCode(e.target.result as string);
          }
        }
      };
      reader.readAsText(file);
    }
  }, [file]);

  return (
    <div className="pl-10 pr-12 max-h-[calc(100vh-188px)]">
      <input type="file" onChange={handleFileChange} />
      <Editor language="yaml" value={code} />
    </div>
  );
};

export default EditorView;
