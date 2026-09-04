'use client';
import {useState} from 'react';

export default function FileKnowledgeImporter({onParsed}:{onParsed:(value:{filename:string;suggestedTitle:string;content:string})=>void}){
  const [busy,setBusy]=useState(false),[error,setError]=useState('');
  async function choose(file?:File){
    if(!file)return; setBusy(true); setError('');
    try{const body=new FormData();body.append('file',file);const response=await fetch('/api/v1/knowledge/file-preview',{method:'POST',body});if(!response.ok)throw new Error(await response.text());onParsed(await response.json())}
    catch(e){setError(e instanceof Error?e.message:'文件识别失败')}
    finally{setBusy(false)}
  }
  return <section className="file-importer"><div><b>从电脑上传资料</b><span>解析后先预览，再进入父子切片和混合索引</span></div><label className={busy?'disabled':''}>{busy?'正在识别…':'选择文件'}<input type="file" accept=".pdf,.docx,.txt,.md,.markdown,.csv" disabled={busy} onChange={e=>void choose(e.target.files?.[0])}/></label>{error&&<p>{error}</p>}</section>
}
