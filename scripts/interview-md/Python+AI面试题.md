### python的基本数据类型和数据容器？
- None类型

### 在接受参数时，*args 和 **kwargs的区别是什么
函数接收实参，按顺序分配给形参，但遇到带*的形参时，就把未分配出去的一起以元组 or 字典形式打包分配给带星号的参数。

### 装饰器、生成器、迭代器、列表推导式？上下文管理器？
装饰器：用来修改或扩展函数方法的高阶函数，接受一个函数作为参数并且返回另一个函数，而不需要修改源代码。例如代码重用、日志记录、性能计时生成器：通过 yield 语句生成一个值。它不会一次返回所有的值，而是每次请求一个值时才生成一个。这种惰性求值可以节省内存。（流式输出）迭代器：实现了 iter 和 next 方法，它允许我们遍历一个集合而不需要暴露集合的内部结构。（例如生成器本身就是一个迭代器）。列表推导式：一般是由一个表达式 + 可选条件 + for 语句构成，用一种简洁的方式来创建基于某种逻辑的列表。squares = [x**2 for x in range (5)] 上下文管理器：通过 with 语句来实现，主要用来自动管理资源，保证用完后能正确释放，比如文件读写和数据库连接等

### 在python中，深拷贝和浅拷贝的区别是什么
浅拷贝是指复制对象的最外层结构，内部引用共享。copy.copy() 深拷贝是递归复制内部的所有元素及其可变对象。copy.deepcopy() 比如创建一个列表嵌套列表，分别使用两种拷贝方法，然后在嵌套列表里面添加一个元素，print结果，会发现，浅拷贝里面的嵌套列表也增加了这个元素，但深拷贝没有。浅拷贝：- 对象内部元素是不可变对象，或不关心内部共享。- 如：复制配置模板，树结构的节点引用深拷贝：- 当对象内部含有可变对象，并且需要完全独立修改，避免改动相互影响- 如：复制嵌套列表、复杂JSON配置

### 项目中什么地方用到了python的迭代器和生成器？
流式对话

### python 这块你有用什么框架吗？web框架用了吗？
python这块我们项目中有用到很多框架，比如说Langchain和LangGraph框架，他们主要是与AI大模型平台进行对接然后Web框架选用的FastAPI来进行接口的开发，供业务系统来调用

### 主流向量数据库的适合场景及优缺点？

常见的有 Milvus、Qdrant、Weaviate、Pinecone、Chroma、pgvector 和 FAISS。Milvus 适合大规模、高并发的生产检索，索引类型丰富，但部署和运维成本较高；Qdrant 轻量、过滤能力强、Rust 实现性能好，适合中小规模和中台场景；Weaviate 自带混合检索和模块化能力，生态完善但资源占用偏高；Pinecone 是全托管服务，免运维、弹性好，但成本高且数据在云端；Chroma 适合本地原型和小规模应用，简单易用但生产级能力有限；pgvector 适合已经使用 PostgreSQL、数据量中等的场景，事务和过滤方便，但超大规模性能不如专用向量库；FAISS 是检索库而非数据库，性能高、灵活，但需要自己实现持久化、增删和权限。选型主要看数据规模、过滤需求、部署方式和运维成本。

### 你们项目中Redis存储都用到了哪些场景？键和值分别都是什么？
用的是string类型, key是问题名称, 值就是回答的内容

### 简述一下向量数据库的原理？
向量数据库的核心原理就是将文本图像此类非结构化数据转为为高维空间的向量, 通常来讲, 语义相似的的内容在向量空间距离更近. 由于企业级数据库存储的数据量很大, 所以在检索的时候往往采用的是ANN 快速近似最近邻搜索, 通过构建索引结构, 快速定位候选集选取出top-k 主流的ANN算法包括HNSW原理是构建多层图, 高层负责搜索范围, 底层则是精细搜索, IVF+PQ原理则是先聚类再压缩, 将向量分成多个簇, 查询时只搜附近的几个簇

### 你知道 Milvus 数据库支持哪些索引吗？

Milvus 支持 FLAT（暴力检索，精度最高、适合小数据量）、IVF_FLAT、IVF_SQ8、IVF_PQ（倒排 + 量化，适合大规模、内存敏感场景）、HNSW（图索引，查询快、召回高，但内存占用较大）、DISKANN（磁盘索引，适合超大规模、低成本场景）、SCANN，以及 GPU_IVF_FLAT、GPU_IVF_PQ、GPU_CAGRA 等 GPU 索引。选择时在召回率、延迟、内存和构建时间之间权衡：小数据量用 FLAT，大规模在线检索常用 HNSW 或 IVF 系列，超大规模且内存有限可以用 DISKANN。

### 你是如何理解相似度索引的？
相似度检索本质上是寻找语义相似的结果，核心思想是将数据转化为高维向量，通过计算向量间数学距离衡量相似性。它的原理是：他会先通过一个模型选择使用不同的相似度度量（余弦相似度-文本，欧式距离-图像常用，内积）将数据转换为向量，最后进行索引加速，然后查询执行主流的索引类型：- FLAT：最简单的索引方式，进行暴力搜索，可以保证精确度，但效率低，尤其在数据量大时。适合在小型、百万级数据集上寻求完全精确的搜索结果- IVF_FLAT：一种基于聚类和倒排索引来进行数据存储和检索的方法，广泛用于在大规模数据集上实现高效的近似最近邻搜索- IVF_PQ：一种高效的向量索引方式，结合了倒排文件索引和乘积量化技术，旨在加速大规模高维数据集的检索- HNSW：基于图的索引，最适合对搜索效率有高要求的场景

### Langchain的基础组件有哪些?
1. Models组件: 提供了与各种模型的集成，并为所有模型提供了一个精简的统一接口. 支持大模型、向量模型以及聊天模型2. Prompts组件: 主要用于构建提示词，可以方便提示词的动态书写3. Chain组件: 通过标准化的接口将离散的组件（如提示模板、模型、外部工具等）串联成一个工作流，多用于处理那些需要多步骤协作的复杂任务4. Agents组件: 使语言模型能够动态决定任务执行流程，通过与工具的交互完成复杂任务5. Memory组件: 实现会话记忆6. Index组件: 主要用来实现RAG，组件中提供了文档加载、文档拆分、文档向量化的工具

### LangChain和LangGraph两个框架有什么优缺点？
LangChain是一个通用的智能体编排工具，它把工作编排成一系列的工作节点，用一条工作链把这些节点连接起来，组织成一张有向无环图(DAG) 它的工作方式适合比较简单的智能体（比如：提示词-->调用模型-->结果优化），一旦涉及到一些复杂的流程，就有些力不从心了 LangGraph是由LangChain团队开发的开源框架，专为构建基于大型语言模型(LLM)的有状态、多参与者应用程序而设计其核心创新在于采用图结构(Graph)作为计算模型，突破了传统有向无环图(DAG)的限制，支持循环工作流和动态状态管理用节点（Node）表示任务步骤，用边（Edge）连接节点，并通过状态（State）在不同节点之间传递信息

### LangChain处理长文本和多轮对话的内部优化机制
LangChain处理长文本和多轮对话，核心就是围绕 LLM 上下文窗口有限这个痛点，我从两大块来说：1. 对于长文本处理的优化机制文本分块：把超长文档按语义层级拆分，用递归分块器保证段落连贯，块之间加重叠避免信息断裂，再给块加元数据方便后续检索。使用RAG把文本向量化存入向量库，问答时只召回最相关片段，而不是把全文塞给模型，从根源解决超长文本问题2. 对于多轮对话的记忆管理机制为了让会话历史中保留核心的信息，而又不至于内容太多，我们可以采用下面几种策略- 滑动窗口记忆：只保留最近 N 轮对话，控制token不爆炸- 摘要记忆：历史太长时，自动用 LLM 把旧对话压缩成摘要，只保留最近几轮完整内容- 摘要 + 窗口混合记忆：近期完整保存、远期摘要合并，平衡细节与长度- 向量检索记忆：把历史对话向量化，变成 “长期记忆”，需要时再召回，实现近乎无限轮对话总之一句话，就是尽量只保留跟当前问题相关的内容和历史发送给大模型

### 假设现在让你基于Function calling制作一个天气查询的功能, 后端代码如何写?

基于Function calling做天气查询的后端核心逻辑分4步：
1. 定义函数描述：定义天气查询函数的入参（城市、日期）、出参（温度、天气状况）
2. 调用大模型：把用户提问 + 函数描述传给大模型，让模型判断是否需要调用天气函数，并返回标准化的调用参数
3. 执行本地函数：解析大模型返回的调用参数，调用真实的天气 API（可以对接高德）获取数据
4. 结果回填：把天气数据传给大模型，让模型生成自然语言回答，返回给用户

### 如果要做到人机交互的之类的体验, 如何在langgraph上面体现出来?
要在LangGraph上做出自然流畅的人机交互体验，主要就三点：1. 把交互做成一个节点：流程跑到需要问用户、确认信息的时候，就加一个人工交互节点，让流程暂停，等用户输入。2. 用状态保存上下文：所有对话信息、用户回答、中间结果，都存在统一的 State 里，这样多轮交互不会断，上下文一直连贯3. 用条件边控制什么时候找人：比如信息不全、不确定、需要确认时，就让流程自动走到交互节点问用户；信息够了就直接往下执行整体一句话：在工作流里插交互节点，用状态存上下文，用条件判断什么时候问用户，就能做出自然的人机交互

### 聊一聊SpringAI和langchain之间的区别
Spring AI 和 LangChain 核心都是大模型应用开发框架，但定位、生态、使用场景完全不同1. 首先，二者的定位和出身不一样- LangChain是通用型大模型编排框架，核心解决LLM + 外部工具（检索、函数调用、记忆）的组合问题，更灵活- SpringAI 是Spring生态专属的大模型框架，本质是把大模型能力融入 Spring 体系，更贴合企业级 Java 开发2. 其次，适用场景也不一样- LangChain适合做灵活的 AI 原生应用（比如多轮对话、RAG 问答、智能体），它的记忆、检索、流程编排能力更成熟- Spring AI适合做企业级 Java 后端 + 大模型的融合应用，特别适合在原有Java程序的基础上添加大模型的场景，不用改原有技术栈，学习成本低总结来看，就是LangChain不限技术栈，灵活但和企业级后端整合成本高；Spring AI只服务Spring开发者，适配企业级开发，上手快、整合成本低

### langgraph构建的agent发布的时候是以什么形式发布
在我们公司中，这个agent的发布形式主要有两种：1. 本地运行：就是在agent之前添加上FastAPI，对外提供一个可通过HTTP访问的接口，然后直接使用python命令运行程序2. 容器化部署：把Agent代码使用Dockerfile打包成docker镜像，然后运行docker容器，对外提供服务

### RAG的原理是什么？
RAG，就是检索增强生成，之所以使用它是因为，大模型有两个典型的缺点就是：知识更新延迟和领域知识缺失为了解决这这两个问题，就有RAG技术，它的做法是在调用大模型的时候，需要将跟问题相关的资料提取出来一起发给大模型，进而提高问题回答准确性它的整个流程分为这样几步：1. 将公司内部的一些资料文档向量化之后存储到数据库备用2. 当用户提问时，先将用户问题通过嵌入模型向量化，然后从向量数据库中召回相关文档3. 将召回的文档和用户的原始问题一起拼装为提示词，然后发送给大模型4. 大模型基于增强后的提示词来输出答案二. 业务场景

### BM25算法原理是什么
BM25是一种用于信息检索的排序算法, 用来衡量一个查询与文档之间的相关性得分它是TF-IDF的改良版, 常被用于搜索引擎比如ES 所谓TF-IDF是指词频-逆文档频率, 它有两个严重缺陷:- 词频线性增长: 当一个词反复出现时, 会让其的分数直线增长- 长文档: 长文档天然包含更多词，更容易命中查询词, 短文档得分先天劣势而BM25的核心思想: 对词频进行非线性压缩, 并对文档长度做归一化首先就是高频词汇的得分会逐渐趋近于k1 + 1 (不再增长), 而文档长度归一化后长文档的得分会降低, 至于IDF部分加入的平滑项避免了除零问题

### 千问模型不是可以接受图形形式的输入吗？为什么还要用OCR技术呢？
在我们的项目中，为了实现RAG功能，需要将公司内部的一些资料，比如说：图片、ppt、pdf等存储到向量数据库在这个过程中，需要提取这些资料中图片里面的文字信息，而OCR技术就是用来完成这个功能的

### 当用户问题超出知识库范畴时, 如何做兜底策略?
如果用户问超出知识库范畴, 当返回的数据不满足条件时, 回复并未找到相关的文档, 并给出人工客服的电话. 写提示词是要杜绝大模型自由发挥, 如果未能返回任何有效的文本直接触发兜底策略

### 在你们项目里面能处理哪几种类型的文档数据?
在我们的项目里面, 能够处理像是.txt文件, .pdf文件, .docx文件, .jpg一类的图片文件和.md文件 .txt文件在加载时是用到了encoding="utf-8"

### 父子块token切分没有遇到过切分出来的内容语义不连贯的问题
根据父子模块设计的大小, 出现此类问题的情况并不多, 而且文件也都是大体浏览过的,不会出现那种非常极端的大块情况. 当然了, 我们也做好了兜底准备,我们在上线前会对切分结果做抽样检查，特别是手册这类结构敏感内容。如果发现某类文档频繁出现断句问题，会为其定制切分规则（比如用正则识别章节标题，强制在标题后切分）步骤如下1. 定义标题模式（根据文档类型定制）；2. 遍历全文，找到所有标题位置；3. 以标题为锚点，将文档分割成“段落块”；4. 对每个段落块再做 token 截断（可选），避免单个块过大

### 是否遇到过多模态的问题?
我们目前的文档加载模块支持多格式输入（PDF、图片、Office 等），并通过 OCR 和解析器将它们统一转换为文本，这是构建 RAG 的基础。但严格来说，这还属于单模态（文本）系统。我了解过多模态, 核心系统能同时理解并融合不同模态的信息（如文字 + 图像语义 + 表格结构）. 我们这个项目的技术没用到这一块, 以后如果有机会用到的话我会做进一步的了解

### 重排的原理是什么？为什么要重排?
向量模型虽然能找到语义相近的内容,但无法实现精确判断, 重排的话他使用的架构是精排模型, 可以计算细粒度的分数, 返回TOK-K的相关父块送入大模型中, 回答会更精确

### 重排TOPK 的打分依据
重排模型的打分是根据(query, documnet)联合语义的匹配度评估, 他们拼在一起后输入到BERT类模型. 简单来说, 不是分别编码看距离远近, 而是联合编码以后看匹配的分数高低, 这个分数是一个相对评分, 因此会有不同的分数, 在本项目里没有设置动态截断, 如果有需要也可可以设定一个最小分数阈值

### RAG返回结果为空，大模型正常返回结果，怎么处理这种情况?
常用的方式就是脱敏降级, 明确地告诉用户查无此文档,并告知根据常识,其结果是....一定要明确地告知用户这不是数据库内提取到的内容而是通用知识搜索来的

### 如果错误发生在LangGraph工作流的中间步骤（比如调用高德地图 API 失败），如何利用Checkpoint机制实现“断点续传”或“状态回滚”？

LangGraph 的 Checkpoint 机制本质是持久化工作流的状态快照，包含：
1. 工作流当前执行到的节点
2. 所有已完成步骤的状态数据
3. 对话 / 工作流的上下文信息实现 “断点续传” 和 “状态回滚” 的核心逻辑是：
- 断点续传：出错时保存当前最新的有效 Checkpoint，恢复时从该 Checkpoint 加载状态，跳过已成功执行的步骤，直接从出错节点重新执行。
- 状态回滚：出错时放弃当前错误状态，加载上一个成功步骤的 Checkpoint，回到出错前的状态，可选择重试该步骤或调整逻辑后重新执行。

### 当使用fastapi承载大模型接口时，你是如何处理那些可能阻塞事件循环的操作的？比如涉及到外部io模型推理或者耗时的数据处理时？你采取过哪些方式来避免阻塞主事件循环？

FastAPI 基于异步的 Starlette 框架，其高性能依赖于非阻塞的事件循环。一旦有同步阻塞操作（如 CPU 密集型计算、无异步接口的外部 IO 调用）占用事件循环，会导致整个服务的并发能力急剧下降。
解决思路分为三类：
1. 异步化改造：优先使用异步库替代同步库，让 IO 操作让出事件循环。
2. 线程 / 进程池隔离：将阻塞操作放到独立的线程 / 进程池中执行，避免占用主事件循环。
3. 服务拆分：将重度阻塞的操作（如大模型推理）拆分为独立服务，通过消息队列异步调用。

### 构建智能体流程的过程中，当系统需要连续经过多个节点才能得到最终结果时，你是如何确保用户能够尽快看到内容的？比如在请求还没完全结束之前，系统是否会提前输出部分结果？或者采取其他方式来提升游戏体验，请结合你当时的实际做法来讲一讲？

在构建智能体流程时，我主要从三个方面保障用户体验：流式输出、分阶段反馈、并行处理与降级策略。
应。当大模型开始生成 token 时，我们就以小数据块的形式逐个返回，用户可以立刻看到‘打字机’ 第二，分阶段输出中间结果。对于多步骤任务（如‘检索 → 分析 → 总结’），我们不会等到全部完成才返回。例如，在检索阶段完成后，先返回‘正在分析文档…’这类友好提示；分析完成后，再展示初步结论。这样用户始终知道系统在做什么。
第三，关键任务并行化(异步)。如果多个子任务是独立的（如同时调用天气 API 和交通 API），我们会并行执行，并采用‘谁先完成先展示’的策略，提升整体感知速度。
第四，设置降级策略兜底。若某环节超时未完成，我们返回简化版答案或默认提示（如‘信息获取中，请稍后重试’），防止用户卡死。

### 调用智能体（Agent）出错时（如 API 超时、异常），你们的系统如何做错误兜底和降级处理？

节点内使用异常处理, 如果工具调用出错会返回友好提示, 比如关键步骤出错会返回"xx功能暂不可用", 对于非核心功能我们会直接熔断只保证核心功能可用, 同时我们会记录日志到监控系统便于之后查看修复如后续追问日志怎么用的: 我们公司做了一个可复用的, 带文件和控制台输出的log文件, 它用的是Python内置的日志模块, 并且读取了我们的配置, 它一般只记录INFO以上级别的日志, 开发时我们才临时调成DEBUG, 它可以同时输出日志到磁盘以及开发时实时查看控制台输出

### 什么是“意图-动作状态机”，并说明它在对话系统中的作用。结合大模型（LLM），你是如何实现这种状态机的？

意图-动作状态机’是一种用于管理任务型对话流程的设计模式，其核心思想是：
将整个对话拆分为若干明确的状态（如‘询问出发地’、‘确认订单’），每个状态对应一个用户意图阶段。当用户输入新消息后，系统根据当前状态 + 用户内容，决定：
- 下一步进入哪个状态
- 执行什么动作（如追问信息、调用工具、结束对话）
一共解决了三个问题
1. 保证流程可控：按业务规则一步步走，避免遗漏关键信息；
2. 提升成功率：在预设路径上引导用户，减少幻觉或无效回复；
3. 便于维护和测试：每个状态逻辑独立，新增/修改不影响整体在实际项目中，我们使用 LangGraph 来实现这种状态机：
- 支持循环（如多次追问）、异常处理（fallback）

### 你能讲一下你对MCP的理解吗？（待定）
答：MCP就是Model Context Protocol（模型上下文协议）,它是用来解决工具调用（Tool Calling。工具其实跟函数调用有区别，工具是外部函数的封装）的碎片化问题，实现”一次实现，多处复用 “。就是说工具提供方只需要实现MCP协议，客户端可以对接任意MCP协议，跟框架没关系。【Host、Client、Server三部分组成】

### 谈一谈对MCP协议的理解
MCP，全称 Model Context Protocol（模型上下文协议），是一种开放标准协议，目标是为了解决大语言模型（LLM）与外部工具、数据源和服务之间通信不统一、集成成本高、生态割裂的问题。工具或服务实现了 MCP 协议，任何支持 MCP 的 LLM 应用（比如智能助手、IDE 插件、多智能体系统）都能即插即用，无需重复开发适配逻辑由于大模型缺乏记忆, 并且本身训练时使用的数据存在过期等问题, 传统做法使用工具封装没有统一标准, 返回的数据结构各种各样. 而MCP协议很好低解决了这个问题 MCP的核心架构一共有四种1.Host(主机): 就是LLM应用本身2.Client(客户端): 负责与Server建立连接3.Server(服务器): 独立运行的工具服务, 提供三类资源Tools, Resources, Prompts4.Base Protocol(基础协议): 一般支持三种消息类型 Request/Response (同步调用), Notification: 异步事件, 支持Stdion(本地)和SSE/HTTP远程两种传输方式它的优势在于:标准化：所有工具暴露schema统一, 大模型更易理解可复用: 一个MCPserver可被多个智能体同时调用动态发现: 智能体启用时可以自动获取可用工具列表, 无需硬编码**延申: MCP和常见的网络协议的区别在哪里?**MCP(Model Context Protocol)是一种面向LLM应用的高层协作协议, 他的目的是将大模型和外部工作, 数据源之间的交互标准化. 不同于HHTP等网络协议, MCP解决的的是"模型如何理解, 并调用外部能力完成复杂任务", 后者更多低关注于"如何传输数据"

### MCP协议中, Resource和Tool的区别
在MCP协议中, tool和Resource都由MCP Server提供定义上来讲, tool属于可执行的操作函数, LLM调用tools时会产生后续的影响比如修改数据等. 而 Resource则是可读取的数据片段, LLM使用Resource时往往只是读取数据. 使用场景上来说, 使用tool时往往会有返回值, 而Resource值只是用于上下文注入延申: 如何做数据脱敏?MCP原生就支持基于URI的细粒度权限空值, 设置了权限白名单, 只有符合白名单的才允许访问, 其次即使有权限也要做数据脱敏, 原始敏感数据不会直接返回而是采用类似于掩码的格式返回诸如手机号一类的数据, 同时MCP Client在请求resource可以携带上下文元数据, Server也可以代码做校验决定用户是否可以访问. 大模型的prompt不可以直接拼接敏感信息, 必须是大模型显式请求而且通过client的校验才允许

### 模型跟MCP是怎么交互的，交互的流程是什么
其中HOST: 是项目中运行LLM的智能体, MCP Client是嵌入再Host中的客户端, 以及MCPServer: 提供 Resource/Tool的服务端选择Resource: LLM可以获取可用的Resource列表, 当用户试图查询数据时, 大模型要生成调用指令, MCP Client可以拦截该指令, 这个要判断一下用户是否有权限访问, 输出前要做数据脱敏三，A2A

### 什么是A2A协议？它解决了什么问题？
A2A（Agent-to-Agent Protocol）是OpenAI提出的Agent间标准化通信协议，定义了不同Agent之间互相调用、协作的统一接口规范。解决的问题：①Agent碎片化：不同厂商/团队开发的Agent无法互通；②重复造轮子：每个Agent都要重新实现工具和能力；③协作困难：没有标准协议导致多Agent集成成本极高。A2A让Agent像HTTP时代的网页一样，可发现、可调用、可组合，形成Agent互联网。

### A2A的核心设计原则是什么？
核心原则：①基于HTTP：复用现有Web基础设施，Agent就是一个HTTP服务；②MCP兼容：底层复用MCP（Model Context Protocol）的消息和工具模型；③异步优先：Agent调用可能耗时久，原生支持任务式异步调用；④人机通用：同一套接口既支持Agent调用也支持人类通过UI使用；⑤可发现：通过Agent Manifest自描述能力；⑥无状态：服务端不保存会话状态，由调用方维护。

### A2A和MCP是什么关系？
MCP（Model Context Protocol）是LLM与工具/数据源之间的协议，解决单个Agent怎么调用外部工具；A2A是Agent与Agent之间的协议，解决Agent之间怎么互相调用。关系：A2A构建在MCP之上，复用了MCP的工具定义、消息结构、资源模型等核心概念，可以理解为「Agent化的MCP」。MCP是工具层协议，A2A是Agent层协议；MCP的server是工具提供者，A2A的server是Agent提供者。

### A2A协议的核心角色有哪些？
两个核心角色：①A2A Client（调用方Agent）：发起调用的Agent，主动发现并调用其他Agent；②A2A Server（被调Agent）：提供能力的Agent服务，对外暴露标准化接口。每个Agent可以同时是 Client和Server，既调用别人也被别人调用。交互模式：Client通过HTTP请求Server，Server返回任务状态或结果，类似REST API但专门适配Agent长任务场景。

### A2A中的Agent Manifest是什么？
Agent Manifest是Agent的自描述文件（JSON格式），类似OpenAPI Spec。通过/.well- known/agent.json路径暴露。内容包括：Agent名称、描述、版本、支持的能力（工具/任务/资源）、认证方式、输入输出Schema等。作用：①自动发现：Client访问URL即可获取Agent全部能力；②动态适配：LLM根据Manifest自动决定怎么调用这个Agent；③版本管理：支持协议版本兼容协商。

### A2A协议定义了哪些核心API端点？
标准端点：①GET /.well-known/agent.json：获取Agent清单；②POST /tasks：创建新任务（异步调用入口）；③GET /tasks/{id}：查询任务状态；④GET /tasks/{id}/stream：流式获取任务进展；⑤POST /tasks/{id}/cancel：取消任务；⑥GET /tools：列出可用工具（同步调用模式）；⑦POST /tools/{name}/call：同步调用单个工具。核心是任务式异步调用，因为Agent执行通常耗时较长，不适合同步等待。

### A2A的同步调用和异步调用有什么区别？
同步调用：直接调用/tools/{name}/call，立即返回结果，适合快速返回的简单操作，类似普通API调用；异步调用：通过/tasks创建任务，返回task_id，轮询或流式获取结果，适合耗时较长的Agent任务（如写报告、复杂推理）。A2A推荐优先用异步任务模式，因为Agent执行时间不可控。异步任务有完整的状态机：pending → running → completed/failed/cancelled。

### Python中如何开发一个A2A Server？
常用方案：①FastAPI + A2A SDK：用FastAPI写HTTP服务，按A2A协议规范实现各端点；②官方a2a Python库：OpenAI官方提供的a2a包，封装了协议细节，快速搭建Server；③LangChain/LangGraph集成：将现有LangGraph Agent包装成A2A服务。最简步骤：定义Agent能力清单 → 实现任务创建和状态管理接口 → 暴露agent.json清单 → 启动HTTP服务。核心是实现tasks 的创建、查询、状态流转。

### Python中如何实现A2A Client调用其他Agent？
实现步骤：①发现：请求目标Agent的/.well-known/agent.json获取能力清单；②选择：LLM根据 Manifest决定调用哪个工具/任务；③调用：同步模式POST /tools/{name}/call传参执行；异步模式 POST /tasks创建任务，获取task_id；④等待：轮询GET /tasks/{id}或订阅stream直到完成；⑤解析：提取结果返回给上层。可用httpx/aiohttp做HTTP客户端，也可用官方a2a SDK的Client类简化调用。

### A2A中的任务状态机是怎样的？
任务标准状态：①pending：已创建等待执行；②running：正在执行中；③completed：执行成功完成，包含结果；④failed：执行失败，包含错误信息；⑤cancelled：被取消。状态流转：pending → running → completed/failed；running → cancelled。Client通过状态判断是否需要继续等待。状态中还可包含steps字段，记录执行的中间步骤，便于调用方Agent追踪进展。

### A2A和多Agent框架（如CrewAI、AutoGen）有什么区别？
A2A是协议标准，不是框架：定义Agent间通信的通用语言，不同语言/框架实现的Agent都能互通，跨平台跨组织；CrewAI/AutoGen是框架：在同一进程/系统内编排多个Agent，强绑定特定语言和实现，适合内部系统。类比：A2A像HTTP协议，CrewAI像单体应用内的函数调用。A2A适合跨团队、跨公司、异构Agent协作；多Agent框架适合单系统内紧密协作的Agent群。

### A2A协议如何处理认证和安全？
认证方式：①API Key：最常用，请求头Authorization: Bearer <key>；②OAuth 2.0：适合企业级场景；③无认证：公开Agent可选。安全机制：①Manifest中声明auth字段，Client自动适配；②能力权限控制：不同密钥可访问不同工具；③输入校验：服务端严格校验输入Schema；④速率限制：防止滥用。生产环境建议用API Key + HTTPS，敏感Agent还要加审计日志和调用白名单。

### A2A中的流式输出（Stream）是怎么实现的？
通过/tasks/{id}/stream端点，采用Server-Sent Events（SSE）协议流式推送。事件类型包括：①task.status：状态变化事件；②task.message：增量消息Token，类似ChatGPT打字效果；③task.step：新的执行步骤；④task.result：最终结果。好处：调用方可以实时展示Agent执行进展，提升体验，不用等全部完成才看到内容。Python可用FastAPI的StreamingResponse实现SSE。

### A2A协议支持哪些类型的能力？
三类核心能力：①Tools（工具）：同步执行的原子操作，如查询天气、计算、数据库查询，立即返回；②Tasks（任务）：异步长耗时操作，如写报告、代码审查、复杂推理，需要轮询状态；③Resources（资源）：可读取的数据源，如文档、知识库、数据集，类似MCP的资源。一个Agent可以同时提供多种能力，全部在Manifest中声明。工具适合快，任务适合慢，资源适合读。

### A2A如何处理Agent调用Agent的嵌套调用？
A2A天然支持多层嵌套：Agent A作为Client调用Agent B，Agent B内部又作为Client调用Agent C，形成调用链。每层都是独立的HTTP请求，各自维护任务状态。注意事项：①超时控制：每层设置合理超时，避免级联等待；②成本控制：嵌套调用Token和费用叠加，需做预算；③错误传递：内层错误要清晰透传到外层；④深度限制：避免无限递归调用。实际中常见2-3层嵌套，过深会显著增加延迟和成本。

### Python中A2A与LangGraph如何结合使用？
结合方式：①LangGraph作为A2A Server：将LangGraph工作流包装成A2A任务接口，外部Agent通过 A2A协议调用；②LangGraph作为A2A Client：在LangGraph节点中调用其他A2A Agent，扩展能力；③混合模式：自身是A2A服务，内部又调用其他A2A Agent。典型实现：LangGraph处理核心业务逻辑，外面套一层FastAPI实现A2A协议端点，任务创建对应graph.invoke，任务状态对应checkpoint查询。

### A2A的错误处理机制是怎样的？
错误分层：①HTTP层：4xx客户端错误（参数错、未授权）、5xx服务端错误；②任务层：任务进入 failed状态，error字段包含code和message；③业务层：工具执行返回的业务错误。标准错误码：INVALID_ARGUMENT（参数无效）、UNAUTHENTICATED（未认证）、PERMISSION_DENIED（无权限）、NOT_FOUND（任务不存在）、INTERNAL（内部错误）、CANCELLED（已取消）。Client 需同时处理HTTP错误和任务失败两种情况。

### A2A协议目前有哪些局限性？
局限性：①协议较新，生态尚不成熟，兼容实现较少；②没有标准服务发现机制，需手动知道Agent URL；③缺乏统一的Agent质量评级和信誉体系；④异步任务轮询效率不高，Webhook回调还在完善；⑤安全与计费没有统一标准，各平台自行实现；⑥复杂多Agent协作模式（如辩论、投票）还需自行设计，协议只定义点对点调用。

### 实际项目中什么时候该用A2A？
适用场景：①跨团队/跨公司Agent协作：不同部门各自开发Agent，需要互通；②Agent能力复用：已有成熟Agent，不想重复开发；③异构系统集成：不同技术栈（Python/JS/Go）开发的Agent需要协作；④构建Agent市场/平台：需要标准化接入第三方Agent。不适用：①单系统内紧密协作的多 Agent，直接用CrewAI/LangGraph更高效；②极简单的工具调用，直接用MCP即可；③对延迟要求极高的实时场景，异步任务模式有开销。

### A2A协议未来的发展方向是什么？
发展方向：①服务发现：Agent目录/搜索引擎，自动发现可用Agent；②质量评级：Agent效果评分、信誉体系；③统一计费：标准化的调用计费和结算；②更丰富的协作模式：广播、订阅、群聊式多 Agent交互；⑤Webhook回调：任务完成主动通知，替代轮询；⑥标准能力模板：常见场景的标准化接口定义；⑦安全沙箱：不可信Agent的安全执行环境。长期目标是形成真正的Agent互联网。三，FastAPI

### FastAPI是什么？它有哪些主要特点？
FastAPI是一个基于Python的现代、高性能Web框架，用于构建API。主要特点：①基于标准Python 类型提示，自动生成文档；②性能接近Node.js和Go，底层用Starlette和Pydantic；③自动生成交互式API文档（Swagger UI/ReDoc）；④原生支持异步async/await；⑤自动数据校验、序列化和反序列化；⑥兼容OpenAPI和JSON Schema标准。

### FastAPI、Flask、Django三者的区别是什么？
FastAPI：异步原生、自动文档、类型提示驱动、高性能，适合构建RESTful API和微服务；Flask：轻量级同步框架、灵活度高、生态成熟，适合小型项目和快速原型；Django：全栈重量级框架、自带 ORM/Admin/认证，适合大型全栈Web应用。性能排序：FastAPI > Flask > Django；开发速度：Django > FastAPI > Flask。

### FastAPI中的路径参数和查询参数有什么区别？
路径参数：写在URL路径中如/items/{item_id}，必填，用于标识资源；查询参数：跟在?后面如/items?skip=0&limit=10，可选，用于过滤/分页。FastAPI通过函数参数自动识别：在路径中声明的是路径参数，其余默认是查询参数；有默认值的查询参数为可选，无默认值的为必填查询参数。

### 解释Pydantic在FastAPI中的作用
Pydantic是FastAPI的数据验证核心库，基于Python类型提示实现。主要作用：①数据校验：自动验证请求体数据类型和格式，不合法返回422错误；②数据序列化/反序列化：自动将JSON转Python模型对象，响应时再转回JSON；③自动生成文档：基于模型生成OpenAPI Schema；④类型安全：IDE 代码提示和类型检查。常用BaseModel定义请求/响应模型。

### FastAPI如何处理请求体（Request Body）？
使用Pydantic模型作为函数参数即可接收请求体。例如：def create_item(item: Item)，其中Item继承BaseModel。FastAPI会自动：①读取请求体JSON；②校验数据是否符合Item模型定义；③校验失败返回422错误详情；④校验成功将数据赋值给item参数；⑤自动生成OpenAPI文档中的请求体 Schema。

### FastAPI中的依赖注入（Depends）是什么？有什么用？
依赖注入是FastAPI的核心功能，用Depends声明函数依赖。作用：①代码复用：共享数据库连接、认证校验等公共逻辑；②关注点分离：业务逻辑和通用逻辑解耦；③安全认证：统一处理Token校验、权限检查；④嵌套依赖：依赖可以继续依赖其他依赖。依赖函数可以是普通函数、async函数、类、生成器（yield用于资源清理）。

### FastAPI如何实现身份认证和授权？
常用方式：①OAuth2 with Password Flow：配合OAuth2PasswordBearer获取Token，用jwt库生成/ 验证JWT；②API Key：在Header或查询参数中传递密钥；③依赖注入统一校验：将认证逻辑封装为依赖，在需要的路由用Depends注入；④Scopes权限控制：OAuth2支持scope细粒度权限。FastAPI 提供fastapi.security模块简化OAuth2实现。

### FastAPI中的响应模型（Response Model）有什么用？
响应模型通过response_model参数指定，作用：①过滤输出字段：只返回模型中定义的字段，保护敏感数据（如密码）；②数据序列化：自动将ORM对象等转为JSON；③文档生成：自动生成响应 Schema；④类型转换：自动处理数据类型转换。常用response_model_exclude_unset=True只返回显式设置的字段。

### FastAPI如何处理文件上传？
使用fastapi的File和UploadFile。小文件可用File(...)接收bytes；大文件推荐UploadFile，它是类文件对象支持异步读写，有filename、content_type等属性。多文件上传用List[UploadFile]。原理是 multipart/form-data编码。注意：文件上传不能同时用JSON请求体，需分开或用Form字段配合。

### 解释FastAPI中的中间件（Middleware）
中间件是处理每个请求前后的钩子函数，用@app.middleware('http')装饰。每个请求都会经过中间件：先执行请求前逻辑，然后调用call_next传递给路由处理，最后执行响应后逻辑。常用于：日志、 CORS、请求计时、全局异常捕获、请求ID注入。注意中间件会影响所有路由，性能敏感逻辑谨慎使用。

### FastAPI中的HTTPException和自定义异常处理器
HTTPException用于主动抛出HTTP错误，可指定状态码、详情、响应头。自定义异常处理器：①定义自定义异常类；②用@app.exception_handler(异常类)注册处理器；③处理器接收request和exc参数返回JSONResponse。还可重写HTTPException、RequestValidationError等内置异常的处理方式，统一错误响应格式。

### FastAPI支持异步吗？如何编写异步接口？
FastAPI原生完全支持异步。在路径函数前加async关键字即可，如async def get_items():。内部可使用await调用异步IO操作（异步数据库、aiohttp等）。FastAPI底层基于Starlette（ASGI框架），异步接口由事件循环调度不会阻塞。注意：异步函数中不要调用阻塞IO（如requests、同步数据库），否则会卡住整个事件循环。

### FastAPI中的APIRouter是什么？有什么用？
APIRouter用于路由模块化拆分，把不同业务的路由分到不同文件。用法：创建router = APIRouter()，用@router.get()注册路由，最后主应用app.include_router(router, prefix='/v1', tags= ['用户'])。好处：①大型项目代码组织清晰；②可统一加前缀、标签、依赖；③支持多版本API；④团队协作互不干扰。

### FastAPI如何实现分页？
常见方式：①查询参数分页：接收skip和limit两个查询参数，数据库查询时切片或用offset/limit；② 依赖注入封装：将分页逻辑封装为分页依赖，所有需要分页的接口直接Depends注入；③响应中返回总数：total、page、page_size、items等字段，可用泛型响应模型统一分页响应格式。Pydantic可配合定义标准分页响应结构。

### 什么是OpenAPI？FastAPI和它的关系？
OpenAPI是描述API的标准规范（原Swagger），定义API的路径、参数、请求响应、认证等元数据。FastAPI自动根据代码（路径函数、类型提示、Pydantic模型）生成符合OpenAPI规范的JSON Schema。基于此自动提供两种交互式文档：Swagger UI（/docs）和ReDoc（/redoc），可直接在线调试接口。

### FastAPI如何连接数据库？有哪些常用方案？
常用方案：①SQLAlchemy 2.0：支持同步和异步(asyncio模式)，ORM方式，最主流；②Tortoise- ORM：原生异步ORM，类似Django ORM风格；③Prisma：类型安全的ORM，支持生成Python客户端；④直接用异步驱动：asyncpg(PostgreSQL)、aiomysql(MySQL)。通常配合依赖注入创建数据库会话，用yield依赖确保连接关闭。

### FastAPI中的BackgroundTasks是什么？使用场景？
BackgroundTasks用于在响应返回后执行后台任务，不阻塞接口响应。用法：在路径函数参数中声明 background_tasks: BackgroundTasks，调用add_task(函数, 参数)添加任务。适用场景：发送邮件、日志写入、数据统计、通知推送等不影响主流程的轻量任务。注意：不是任务队列，进程重启会丢失，重任务建议用Celery/RQ。

### FastAPI如何处理CORS跨域问题？
使用fastapi.middleware.cors的CORSMiddleware。配置项：allow_origins（允许的源，可用通配符全部允许）、allow_credentials（是否携带Cookie）、allow_methods（允许的HTTP方法）、 allow_headers（允许的请求头）。原理是自动添加CORS相关响应头，并处理OPTIONS预检请求。生产环境建议精确配置允许的域名，不要全开。

### FastAPI中的Cookie和Header参数如何获取？
用FastAPI的Cookie和Header特殊参数。Cookie：def read(cookie_param: str = Cookie(default=None))；Header：def read(user_agent: str = Header(default=None))。Header参数会自动将连字符转下划线（User-Agent → user_agent），不区分大小写。也可直接从Request对象中读取：request.cookies、request.headers。

### FastAPI如何进行单元测试？
使用TestClient（基于httpx），从fastapi.testclient导入。用法：client = TestClient(app)，然后 client.get()、client.post()等发起请求，断言响应状态码和内容。支持测试异步接口，TestClient内部自动处理。pytest配合使用，可用fixture共享客户端。注意测试上传文件、认证Header等特殊场景的传参方式。四，LangChain

### LangChain是什么？它的核心作用是什么？
LangChain是一个大语言应用开发框架，用于简化LLM应用的构建。核心作用：①将LLM与外部数据、工具连接起来；②提供链式调用组合多个组件；③封装通用模式（问答、摘要、Agent等）降低开发门槛；④支持向量数据库、记忆、工具调用等能力。核心组件：Models、Prompts、Chains、 Memory、Agents、Retrievers。

### LangChain的六大核心组件分别是什么？
①Models：封装各种LLM和Embedding模型的调用接口；②Prompts：提示词模板管理，支持变量替换和格式化；③Chains：链式调用，将多个组件按顺序串联；④Memory：对话记忆，存储历史上下文实现多轮对话；⑤Retrievers：检索器，从向量库等外部数据源检索相关文档；⑥Agents：智能体，让LLM自主决定调用哪些工具完成任务。

### LangChain中的Chain是什么？有哪些常用Chain？
Chain是LangChain的核心抽象，将多个步骤按顺序组合成流水线。常用Chain：①LLMChain：最基础的Prompt+LLM组合；②SequentialChain：顺序串联多个Chain，前一个输出作后一个输入；③RouterChain：根据条件路由到不同子链；④RetrievalQA：检索增强问答链；⑤ConversationalRetrievalChain：带记忆的检索问答；⑥TransformChain：自定义数据转换。

### 解释LangChain中的RAG（检索增强生成）流程
RAG流程：①文档加载：用Document Loader加载各种格式文档；②文档分割：用Text Splitter将长文档切成小块；③向量化：用Embedding模型将文本块转为向量；④向量存储：存入Vector Store（如 Chroma、Pinecone）；⑤检索：用户提问时，将问题向量化后从向量库检索最相似的Top K文档块；⑥生成：将检索到的文档和问题一起拼入Prompt，传给LLM生成答案。核心是用外部知识增强LLM回答准确性。

### LangChain中的Memory（记忆）有哪些类型？
常用记忆类型：①ConversationBufferMemory：完整保存所有对话历史，简单但Token消耗大；②ConversationBufferWindowMemory：只保留最近K轮对话，滑动窗口；③ConversationSummaryMemory：用LLM自动总结历史对话，节省Token；④ConversationSummaryBufferMemory：结合窗口+总结，超过Token阈值就总结旧对话；⑤ConversationTokenBufferMemory：按Token数限制记忆长度；⑥Entity Memory：提取对话中的实体信息存储。

### LangChain中的Agent是什么？工作原理是怎样的？
Agent是让LLM自主决策的智能体，能根据用户问题选择调用工具。工作原理（ReAct模式）：①Thought：LLM思考当前问题需要什么工具；②Action：选择并调用某个工具；③Observation：获取工具返回结果；④重复思考-行动-观察循环，直到判断可以直接回答；⑤Final Answer：给出最终答案。核心是LLM作为推理大脑，工具作为执行手脚。

### LangChain中常用的文档加载器和分割器有哪些？
文档加载器：①PyPDFLoader/UnstructuredPDFLoader：加载PDF；②TextLoader：纯文本；③CSVLoader：CSV文件；④WebBaseLoader：网页；⑤NotionLoader/ConfluenceLoader：知识库；⑥DirectoryLoader：批量加载目录。文档分割器：①RecursiveCharacterTextSplitter：最常用，递归按分隔符分割，保持语义完整；②CharacterTextSplitter：按固定字符数分割；③TokenTextSplitter：按Token数分割；④MarkdownTextSplitter：按Markdown标题分割。

### LangChain中的Embedding是什么作用？常用模型有哪些？
Embedding将文本转化为高维向量，语义相近的文本向量距离近。作用：①语义检索：计算问题和文档的相似度；②RAG的核心环节：文档入库和检索都需要向量化。常用模型：①OpenAI text- embedding-ada-003；②HuggingFace开源模型（BGE、M3E、GTE等中文效果好）；③Cohere、 Voyage等商用模型。维度一般从几百到几千维不等。

### LangChain支持哪些向量数据库？各有什么特点？
常用向量库：①Chroma：轻量级本地向量库，Python原生，适合开发测试；②FAISS：Meta开源，纯内存，速度极快，需自行持久化；③Pinecone：全托管云服务，免运维，适合生产；④Milvus：开源分布式向量库，支持大规模数据；⑤Weaviate：开源带GraphQL接口；⑥PGVector：PostgreSQL 扩展，关系型+向量合一。选择依据：数据规模、部署方式、运维能力。

### LangChain的Prompt Template怎么用？有什么优势？
PromptTemplate用占位符定义模板，运行时传入变量填充。优势：①提示词复用：同一模板多处调用；②关注点分离：业务代码和提示词解耦；③便于管理和版本控制；④支持Few Shot示例注入。基础用法：PromptTemplate.from_template('解释{concept}，用{style}风格')，然后 format(concept='RAG', style='通俗')。还有ChatPromptTemplate适配对话模型的消息格式。

### LangChain中Output Parser是什么作用？常用的有哪些？
Output Parser将LLM的文本输出解析为结构化数据（字典、列表、Pydantic对象等）。解决LLM输出格式不稳定问题。常用：①PydanticOutputParser：解析为Pydantic模型对象，最常用；②JsonOutputParser：解析为JSON字典；③CommaSeparatedListOutputParser：解析为逗号分隔列表；④DatetimeOutputParser：解析为日期时间；⑤RetryOutputParser：解析失败时自动重试。会自动将格式要求注入Prompt。

### 解释LangChain中的Retriever（检索器）
Retriever是检索相关文档的抽象接口，输入查询字符串，返回相关文档列表。最常用的是 VectorStoreRetriever（基于向量库的相似度检索）。检索方式：①similarity_search：纯相似度检索；②mmr：最大边际相关性，兼顾相关性和多样性；③similarity_score_threshold：按相似度阈值过滤。Retriever是RAG和Chain之间的桥梁，可直接接入RetrievalQA等链。

### LangChain LCEL是什么？有什么优势？
LCEL（LangChain Expression Language）是LangChain的声明式组合语法，用管道符|串联组件。优势：①声明式写法更简洁可读；②原生支持流式输出、异步、批处理；③自动支持并行执行；④统一接口，所有组件都可管道化；⑤方便调试和监控。示例：chain = prompt | llm | output_parser。LCEL是LangChain v0.1后的推荐写法，逐步替代传统Chain类。

### LangChain中如何实现多轮对话的RAG？
用ConversationalRetrievalChain实现，核心流程：①接收用户新问题和历史对话；②先用LLM将历史对话+新问题重写为独立的查询问题（消除指代）；③用重写后的问题去向量库检索相关文档；④将检索到的文档+重写后的问题+历史对话一起传给LLM生成最终回答。关键是问题重写（condense question）步骤，解决「它、这个」等指代问题。

### LangChain中的Tool和Toolkits是什么？
Tool是Agent可调用的单个工具，封装了具体功能和描述，LLM通过描述判断何时调用。常用内置 Tool：搜索引擎（SerpAPI、Tavily）、Python REPL、计算器、SQL数据库、文件读写等。Toolkit是一组相关工具的集合，开箱即用，如SQLDatabaseToolkit（SQL相关工具集）、CSVAgentToolkit、 GmailToolkit、GitHubToolkit等。也可通过@tool装饰器自定义工具。

### LangChain的Callback机制有什么用？
Callback是LangChain的钩子机制，在LLM调用、Chain执行、Tool调用等各个节点触发回调函数。用途：①日志记录：追踪每一步执行情况；②Token用量统计：计算调用成本；③流式输出：实时打印 LLM生成的Token；④监控与告警：异常时通知；⑤调试：查看中间步骤。常用：StdOutCallbackHandler（打印到控制台）、StreamingStdOutCallbackHandler（流式输出），也可自定义CallbackHandler。

### LangChain中如何处理长文档的摘要？
常用三种方式：①Stuff：把所有文档全部塞进Prompt一次生成摘要，简单但受Token限制，只适合短文档；②Map-Reduce：先分块对每块生成摘要（Map阶段），再把所有小块摘要合并成最终摘要（Reduce阶段），适合长文档；③Refine：逐块迭代，先对第一块生成摘要，再把上一轮摘要和下一块一起输入优化摘要，逐步精炼，质量高但速度慢，串行执行。

### LangChain和LlamaIndex有什么区别？
LangChain：通用LLM应用框架，覆盖面广，Agent、Chain、Memory、工具调用等能力强，适合构建复杂的多功能LLM应用；LlamaIndex：专注于数据连接和RAG场景，索引类型丰富（树索引、关键词索引、知识图谱索引等），文档处理和检索优化更深入，适合以RAG为核心的场景。实际项目常配合使用：LlamaIndex做数据索引检索，LangChain做上层编排和Agent。

### LangChain中SQL Database Chain的工作原理？
SQL Chain让LLM自然语言查询数据库。流程：①用户用自然语言提问；②LLM根据数据库表结构 Schema生成SQL查询语句；③执行SQL拿到结果；④LLM根据SQL结果生成自然语言回答。安全风险：LLM可能生成危险SQL（DROP/DELETE），生产环境需用只读权限账号，或用 SQLDatabaseToolkit配合Agent做更安全的控制。也可加白名单校验。

### LangChain生产环境部署需要注意什么？
关键点：①Token成本控制：缓存重复查询、优化Prompt长度、选合适模型；②速度优化：异步并发、向量库索引优化、Embedding批量处理；③稳定性：重试机制、降级策略、超时控制；④可观测性：LangSmith/Langfuse追踪每一步调用；⑤安全：Prompt注入防护、敏感数据脱敏、工具调用权限控制；⑥记忆持久化：用Redis/数据库替代内存记忆；⑦向量库选型：从小规模Chroma过渡到生产级Pinecone/Milvus。五，LangGraph

### LangGraph是什么？和LangChain是什么关系？
LangGraph是LangChain官方推出的基于有向图的Agent构建框架，用于构建有状态、多节点、可循环的复杂工作流。与LangChain关系：LangChain是通用LLM组件库，LangGraph是专门的编排层，补充了LangChain Chain只能线性执行、不支持循环的短板。LangGraph底层复用LangChain的组件（LLM、Tool、Memory等），但用图结构重新定义了执行流程，更适合构建复杂Agent。

### LangGraph相比传统LangChain Chain有什么优势？
传统Chain是线性单向执行，不支持循环、条件分支、复杂状态管理。LangGraph优势：①原生支持循环：Agent可以反复思考-调用工具，适合多步推理任务；②有状态管理：内置状态对象，节点间数据传递清晰；③灵活的控制流：条件边、分支、并行执行；④可中断与恢复：支持人类介入（Human- in-the-loop），执行到检查点可暂停等人工确认；⑤可观测性：每一步状态变化可追踪；⑥原生持久化：检查点机制支持断点续跑。

### LangGraph的核心概念有哪些？
核心概念：①State（状态）：图的共享数据对象，所有节点读写的中心数据结构；②Node（节点）：图中的计算单元，接收State返回State更新，通常是调用LLM或工具；③Edge（边）：连接节点的路径，定义执行流向；④Start/End：起点和终点节点；⑤Conditional Edge（条件边）：根据状态动态决定下一个节点，实现分支逻辑；⑥Checkpoint（检查点）：保存图执行状态快照，支持中断恢复和回溯。

### LangGraph中的State是如何工作的？
State是LangGraph的核心数据结构，是所有节点共享的状态对象。通常用TypedDict或Pydantic定义，包含messages、next_step、result等字段。每个节点接收当前State作为输入，执行计算后返回 State的增量更新（不是全量替换），框架自动合并到全局State中。State支持不同的 reducer 策略：messages用追加模式，普通字段用覆盖模式。整个图的执行过程就是State不断演化的过程。

### 解释LangGraph构建一个Agent的基本步骤
标准步骤：①定义State：用TypedDict声明状态结构，如包含messages列表；②定义节点函数：每个节点接收State，调用LLM/Tool处理后返回State更新；③定义路由函数：条件边的判断函数，根据 State决定下一跳；④创建StateGraph实例；⑤添加节点：add_node('agent', agent_node)；⑥添加边：add_edge连接起点、节点、终点；⑦添加条件边：add_conditional_edges实现分支；⑧设置入口和出口；⑨编译图：graph = workflow.compile()，得到可执行的图；⑩调用执行：graph.invoke(input_state)。

### LangGraph中的条件边（Conditional Edge）是什么？
条件边根据当前State动态决定下一个执行节点，是实现分支逻辑的关键。用法：定义一个路由函数，输入是State，返回下一个节点的名称字符串。用add_conditional_edges(from_node, route_func)注册。典型场景：Agent判断是否需要继续调用工具→调用工具节点，还是直接回答→结束。条件边让图不再是固定流程，而是具备动态决策能力，这是Agent循环的基础。

### LangGraph如何实现Agent的思考-行动循环？
通过节点+条件边形成闭环实现：①Agent节点：调用LLM判断下一步是调用工具还是给出最终答案，将思考结果写入State；②条件路由：根据LLM返回的tool_calls判断，如果有工具调用则走工具节点，没有则走结束；③工具执行节点：执行工具调用，将结果追加到State的messages中；④工具执行完后通过边回到Agent节点，LLM结合工具结果继续思考；⑤如此循环直到LLM决定给出最终答案。这就是经典的ReAct Agent循环。

### LangGraph中的检查点（Checkpoint）有什么作用？
Checkpoint是LangGraph的持久化机制，在每个节点执行后保存状态快照。作用：①中断恢复：图执行中断后可以从检查点继续，不用从头开始；②人类介入（Human-in-the-loop）：执行到指定节点暂停，等待人工审核或输入后再继续；③回溯与调试：可以回看每一步的状态变化；④支持多会话：不同线程/用户的状态独立存储。常用MemorySaver做内存存储，生产可用SqliteSaver或 PostgresSaver持久化。

### LangGraph中的Human-in-the-loop怎么实现？
人类介入通过检查点+中断机制实现。方式一：在编译图时指定interrupt_before或interrupt_after节点，执行到该节点前/后自动暂停；方式二：节点中主动抛出Interrupt异常暂停。暂停后可用 graph.get_state()查看当前状态，人工修改状态后用graph.invoke(None, config)或graph.resume()继续执行。典型场景：敏感操作人工确认、人工修正Agent错误、人工补充信息后继续执行。

### LangGraph和AutoGPT、CrewAI等Agent框架有什么区别？
LangGraph：底层编排框架，灵活度最高，需要自己设计节点和流程，适合定制化复杂Agent，官方出品与LangChain生态无缝集成；AutoGPT：全自动Agent，开箱即用但黑盒，适合通用任务但可控性差；CrewAI：多角色协作框架，聚焦多个Agent分工协作，抽象层级更高，适合团队式Agent场景。简单说：LangGraph是「底层引擎」，CrewAI是「多Agent框架」，AutoGPT是「成品应用」。

### LangGraph中如何处理工具调用？
工具调用标准模式：①定义Tool：用@tool装饰器或Tool类封装工具函数；②Agent节点：调用绑定了 tools的LLM（bind_tools），LLM决定是否调用工具；③工具执行节点：用ToolNode（LangGraph内置）统一执行tool_calls中的工具，自动将结果追加到messages；④条件边：判断是否有tool_calls，有则走ToolNode，没有则结束。ToolNode是内置的工具执行节点，支持批量执行多个工具调用，无需自己写执行逻辑。

### LangGraph中的并行执行怎么实现？
两种并行方式：①扇出扇入：从一个节点出发多条边指向不同节点，这些节点并行执行，全部完成后汇聚到下一个节点。用add_edges_from(['A','B'], 'C')实现汇聚，A和B并行执行完后才执行C；②工具并行：一次LLM调用返回多个tool_calls，ToolNode会并行执行所有工具调用。注意并行节点各自读写State，最终合并时要注意冲突，messages追加模式没问题，普通字段可能被覆盖。

### LangGraph支持流式输出吗？
支持多种流式模式：①stream：按节点流式输出，每完成一个节点yield一次该节点的状态更新；②stream_events：细粒度事件流，包括LLM Token、工具开始结束等所有事件；③stream_log：详细日志流；④a-stream：异步流式。最常用的是stream模式，前端可以逐步展示Agent的思考过程、工具调用、最终结果，提升用户体验。也支持单独流式某个节点的LLM输出。

### LangGraph中的MemorySaver是什么？
MemorySaver是LangGraph内置的内存检查点存储，将执行状态保存在内存字典中。用法：创建 MemorySaver实例，编译图时传入checkpointer=memory_saver。每次调用时传config= {'configurable': {'thread_id': 'xxx'}}，不同thread_id状态隔离。适合开发测试和单会话场景，进程重启数据丢失。生产环境推荐用SqliteSaver、PostgresSaver或自定义持久化存储，保证状态不丢失。

### LangGraph如何实现多Agent协作？
常见模式：①主管-工人模式：一个调度Agent分配任务给多个专业Agent，汇总结果；②顺序协作：多个Agent按流水线依次处理；③辩论模式：多个Agent对同一问题分别给出观点，最后综合。实现方式：每个Agent是一个子图或子节点，通过条件边或路由节点调度。也可用Supervisor模式：一个 Supervisor节点决定下一个由哪个Agent执行，形成循环。LangGraph官方有Multi-Agent协作的完整示例。

### LangGraph中的END节点是什么？
END是LangGraph的内置终止节点，代表图执行结束。当一条边指向END时，图停止执行，返回最终 State。设置方式：workflow.set_entry_point('start')设置起点，边的目标写END即可。也可以条件边返回END字符串来动态终止。注意：图必须有明确的终止条件，否则会无限循环。Agent场景通常是 LLM决定不需要再调用工具时，路由到END结束。

### LangGraph和LangChain Agent的区别是什么？
LangChain自带的AgentExecutor是封装好的黑盒Agent，简单易用但定制化困难，内部执行逻辑不可控，难以加人类介入、复杂分支、持久化等。LangGraph Agent是白盒图结构，每一步都是显式节点，完全可控：可以自定义循环逻辑、插入人类审核、添加分支判断、持久化状态、并行执行等。简单Agent用LangChain AgentExecutor快速搭建，复杂生产级Agent推荐用LangGraph。

### LangGraph中如何做错误处理和重试？
方案：①节点内部try-catch：节点函数中捕获异常，将错误信息写入State，由后续节点处理；②重试装饰器：对工具调用或LLM调用加重试逻辑（如tenacity库）；③条件边重试：检测到失败状态时，条件边路由回当前节点重新执行；④ fallback节点：主节点失败时走备用节点；⑤全局异常捕获：图级别捕获异常，返回友好错误。工具调用的错误通常将错误信息返回给LLM，让它自行修正后重试。

### LangGraph的典型应用场景有哪些？
①智能客服Agent：多轮对话+工具调用+知识库检索；②研究助手：自主搜索、阅读、总结、撰写报告；③代码助手：分析代码、调用工具修复Bug、多步调试；④工作流自动化：审批流、数据处理流水线、需要人工确认的流程；⑤多Agent协作系统：专家会诊、内容创作团队、角色扮演游戏；⑥需要可观测和人工干预的生产级Agent应用。核心是需要循环、分支、状态持久化的复杂LLM工作流。

### LangGraph编译（compile）时可以配置哪些参数？
常用编译参数：①checkpointer：检查点存储，如MemorySaver，开启持久化和中断；②interrupt_before：执行到这些节点前自动暂停，列表形式；③interrupt_after：执行完这些节点后自动暂停；④debug：开启调试模式，打印更多日志；⑤retry_on_error：是否自动重试失败的节点；⑥recursion_limit：最大循环次数，防止死循环，默认25。其中checkpointer和interrupt是Human- in-the-loop的核心配置，recursion_limit是安全必备。六，RAG

### 什么是RAG？它解决了什么问题？
RAG（Retrieval-Augmented Generation，检索增强生成）是将信息检索与大模型生成结合的技术框架。核心流程：先从外部知识库检索相关文档，再将检索到的上下文连同问题一起传给LLM生成答案。解决的问题：①LLM知识截止日期问题，无法获取最新信息；②幻觉问题，LLM可能编造事实；③ 私有数据问题，企业内部数据不在训练集中；④可追溯性，答案有来源依据。是目前企业落地LLM应用最主流的方案。

### RAG的完整工作流程是怎样的？
分两大阶段：【索引阶段】①文档加载：用Loader读取PDF/Word/网页等各种格式文档；②文档切分：用Text Splitter将长文档切成语义完整的小块；③向量化：用Embedding模型将每个文本块转为向量；④向量存储：存入向量数据库，建立索引。【查询阶段】①用户提问；②问题向量化：将问题转为向量；③相似度检索：从向量库召回Top K最相关的文档块；④Prompt拼装：将检索到的文档+问题+指令拼成Prompt；⑤LLM生成：大模型基于上下文生成答案。

### RAG中的文档分块（Chunking）有哪些策略？
常用分块策略：①固定大小分块：按字符数/Token数切，简单但可能切断语义；②递归字符分块：RecursiveCharacterTextSplitter，优先按段落、句子、单词递归切分，保持语义完整，最常用；③语义分块：根据语义相似度切分，遇到语义转折才切；④结构化分块：按Markdown标题、HTML标签、文档章节等结构切分；⑤句子窗口分块：以句子为单位，检索时扩展上下文窗口；⑥父子分块：小块检索精确匹配，大块返回完整上下文。选择依据：文档类型、Embedding模型上下文窗口、检索精度要求。

### 常用的文本分块大小怎么选择？有什么影响？
分块大小是RAG效果的关键参数。常见范围：小块256-512 Token，大块1024-2048 Token。影响：① 太小：语义不完整，单块信息不足，检索到也回答不了；②太大：单块噪音多，相似度计算不准，且可能超出LLM上下文。经验值：通用场景512-1020 Token效果较好。选择原则：信息密度高的文档（如代码、表格）用小块；叙述性文档用大块。也可用混合策略：小块做检索，返回时扩展上下文。

### 什么是Embedding？RAG中如何选择Embedding模型？
Embedding是将文本映射为高维向量的过程，语义相近的文本向量距离近。RAG中Embedding质量直接决定检索准确率。选型考虑：①语言：中文场景优先BGE、M3E、GTE等中文优化模型；通用场景用 OpenAI ada-003；②维度：维度越高表达力越强但存储和计算成本越高，常见1024/1536维；③向量数据库兼容性；④部署方式：开源可本地部署 vs 商用API。中文效果排序（开源）：BGE-large > M3E > GTE > text2vec。还要注意：查询和文档必须用同一个Embedding模型。

### 向量相似度计算有哪些常用方法？
常用距离度量：①余弦相似度（Cosine Similarity）：衡量向量方向的相似性，不受向量长度影响，最常用，值越大越相似（-1到1）；②欧氏距离（L2 Distance）：向量空间中的直线距离，值越小越相似；③点积（Dot Product）：向量对应元素相乘求和，与向量长度正相关；④曼哈顿距离（L1）。选择：归一化后的向量余弦相似度等价于点积，大部分向量数据库默认余弦相似度。文本检索场景一般用余弦相似度。

### RAG中常用的向量数据库有哪些？如何选型？
主流向量库：①Chroma：轻量级Python原生，本地文件存储，适合开发测试和小规模；②FAISS：Meta开源，纯内存，速度极快，需自行持久化，适合大规模批量检索；③Pinecone：全托管云服务，免运维，支持分布式，生产级；④Milvus：开源分布式向量数据库，支持十亿级数据，企业级；⑤Weaviate：开源，自带GraphQL和丰富过滤功能；⑥PGVector：PostgreSQL扩展，关系型+向量合一，适合已有PG的项目。选型维度：数据规模、部署方式（云/本地）、运维能力、是否需要元数据过滤。

### 什么是MMR检索？和普通相似度检索有什么区别？
MMR（Maximal Marginal Relevance，最大边际相关性）是一种兼顾相关性和多样性的检索算法。普通相似度检索只按相似度排序，返回的Top K可能内容高度重复；MMR在相似度基础上加入多样性惩罚，选择与已选内容差异大的文档。公式：MMR = λ * Sim(query, doc) - (1-λ) * max(Sim(doc, selected))。λ控制相关性和多样性的平衡，λ=1退化为纯相似度。适合：检索结果同质化严重、需要多角度覆盖问题的场景，能提升召回的全面性。

### RAG中的召回率和精确率分别指什么？怎么优化？
召回率（Recall）：相关文档中被成功检索出来的比例，衡量「有没有漏」；精确率（Precision）：检索出来的文档中真正相关的比例，衡量「准不准」。优化召回：增大Top K、优化分块策略、用混合检索、增加同义词扩展、问题重写；优化精确：优化Embedding模型、用reranker重排、元数据过滤、调整分块大小。两者是权衡关系：K越大召回越高但精确率越低。RAG一般优先保证召回，再用重排提升精确率。

### 什么是Reranker？在RAG中有什么作用？
Reranker（重排模型）是对初筛结果进行二次排序的模型。流程：先用向量检索快速召回Top 50-100 个候选，再用Reranker精细计算每个候选与问题的相关性，重新排序取Top 3-5给LLM。作用：①提升检索精度：交叉编码器比向量相似度计算更准确；②解决Embedding的语义模糊问题；③用小代价大幅提升整体RAG效果。常用模型：BGE-Reranker、Cohere Rerank、CrossEncoder。重排是RAG优化中ROI最高的手段之一。

### 什么是混合检索（Hybrid Search）？
混合检索结合向量检索（语义匹配）和关键词检索（词法匹配），取长补短。向量检索擅长语义理解、同义词匹配；关键词检索（BM25）擅长精确术语、专有名词、数字。两者结果融合后排序，综合效果优于单一检索。实现方式：①分别用向量库和ES检索，再加权融合；②向量数据库原生支持，如 Pinecone Hybrid Search、Weaviate的BM25。适合场景：专业领域文档、包含大量专有名词和数字的场景，纯语义检索容易漏关键词匹配的结果。

### RAG中如何处理用户的多轮对话？
多轮对话RAG的核心问题是「指代消解」，用户可能说「它是什么」「详细说说」。方案：①问题重写（Query Rewriting）：用LLM将历史对话+当前问题重写为一个独立完整的问题，再去检索；②ConversationalRetrievalChain：LangChain内置的带记忆检索链，自动做问题压缩；③将历史对话一起向量化检索（效果较差）。最佳实践是问题重写方案：既保证检索准确，又不会让检索上下文被历史对话污染。重写和回答可以用不同模型优化成本。

### 高级RAG（Advanced RAG）有哪些常用优化技术？
高级RAG优化方向：①查询优化：问题重写、查询分解、多查询生成（Multi-Query）；②检索优化：混合检索、MMR、Reranker重排、父文档检索；③分块优化：语义分块、父子分块、句子窗口；④后处理：上下文重排（Lost in the Middle优化）、相关度过滤；⑤生成优化：引用溯源、Self-RAG自检、Chain-of-Thought检索；⑥索引优化：结构化元数据、预分类标签。核心思路是从「检索更准」和「生成更好」两端同时优化，而不是只换Embedding模型。

### 什么是Multi-Query检索？
Multi-Query（多查询生成）是查询优化技术：用LLM将原始问题改写为多个不同角度的相似查询，分别检索后合并结果。原理：单个问题的表述可能有偏差，多个角度查询能覆盖更多相关文档。优点：提升召回率，减少因提问方式导致的漏检；缺点：检索成本乘以N倍，需要去重。适合复杂、模糊的问题。常与Reranker配合：多查询扩召回，重排提精度。是LangChain中内置的一种检索器包装。

### 什么是Query Transformation？有哪些常见方式？
查询变换是对用户原始问题进行优化后再检索，提升召回质量。常见方式：①问题重写（Rewrite）：把口语化、指代的问题改写为标准检索问句；②步骤分解（Step-Back Prompting）：把复杂问题拆成多个子问题分别检索；③HyDE（Hypothetical Document Embeddings）：让LLM先生成一篇假设的答案文档，用假设文档去检索，比直接用问题检索效果好；④多查询生成：生成多个角度的查询；⑤查询扩展：补充同义词、相关术语。是提升RAG效果的低成本有效手段。

### RAG如何做效果评估？有哪些指标？
评估分检索质量和生成质量两部分。检索指标：①Recall@K：Top K中命中相关文档的比例；②Precision@K：Top K中相关文档占比；③MRR：第一个相关文档的排名倒数均值；④NDCG：考虑排名位置的加权指标。生成质量指标：①Faithfulness（忠实度）：答案是否都有上下文支撑，无幻觉；②Answer Relevance（答案相关性）：答案是否回答了问题；③Context Precision：检索到的上下文是否相关。工具：RAGAS、TruLens、DeepEval等自动化评估框架。

### 什么是RAGAS？怎么用？
RAGAS（Retrieval Augmented Generation Assessment）是专门的RAG自动化评估框架。核心指标：①Faithfulness：基于上下文验证答案是否有幻觉；②Answer Relevance：答案与问题的相关度；③Context Precision：检索上下文的精确率；④Context Recall：检索上下文的召回率。原理：用 LLM作为评判器，自动打分。使用方式：准备好问题、答案、检索上下文、标准答案（可选），调用 ragas.evaluate()即可得到各维度分数。支持无参考答案的评估，适合持续监控RAG系统质量。

### RAG中常见的幻觉问题怎么缓解？
缓解幻觉的方法：①提升检索质量：确保检索到的上下文足够相关完整，LLM有足够信息回答；②Prompt约束：明确要求「只根据给定上下文回答，不知道就说不知道」；③引用溯源：要求答案标注来源，生成后校验引用是否存在；④Self-Check：让LLM自检答案是否都有上下文支撑；⑤多路验证：多次生成对比一致性；⑥降低温度：temperature设为0，减少随机性；⑦忠实度评估：用RAGAS 等工具自动检测幻觉。完全消除很难，但可以大幅降低概率。

### 什么是Self-RAG？
Self-RAG是让LLM在生成过程中自我决定是否需要检索、检索什么的RAG变体。传统RAG是固定的先检索再生成；Self-RAG是生成过程中按需检索：LLM生成时判断当前知识是否足够，不够就主动触发检索，拿到结果后继续生成。实现方式：训练或提示LLM输出特殊的检索Token，遇到检索Token就执行检索，结果注入后继续生成。优势：简单问题不检索节省成本，复杂问题自动检索保证质量，更灵活高效。

### 生产环境部署RAG系统需要考虑哪些问题？
生产落地关键点：①性能优化：Embedding批量处理、向量库索引优化、缓存热点问题；②成本控制：Embedding和LLM调用成本优化，小模型做检索大模型做生成；③数据更新：增量索引、定时全量重建、文档版本管理；④可观测性：检索日志、答案质量监控、用户反馈收集；⑤安全合规：数据脱敏、权限控制、敏感数据过滤；⑥可靠性：重试、降级、超时控制；⑦评估体系：离线+在线评估，持续迭代效果；⑧扩展性：向量库集群、Embedding服务横向扩容。
