# 云笔记项目
   云笔记是在吸收了多年的运营数据，经过精确的大数据分析指导需求模型的建立从而实现的，云笔记通过大数据分布式存储解决方案解决了笔记信息量膨胀的问题，并且通过分布式搜索引擎服务以及数据分析推荐平台的建立提升了用户搜索分享比较的能力，系统可以有针对性的提供用户其他人分享的笔记资源扩充自己的知识行囊，并且在分享之外通过每个人的配额管理实现了非分享笔记的安全私有化，保证了用户的私人空间。
   
   通过分布式解决方案用户空间被设计为无限大。业务端同时提供了windows客户端，更加贴近用户的使用习惯，并且数据平台支持移动设备的接入，达到无处不学习，无处不笔记的效果。
## 设计理念
   将云笔记信息分别存储在redis和hbase中。
   redis（缓存）：存储每个用户的笔记本信息
   hbase（持久层）：存储用户的笔记本信息、笔记本下的笔记列表、笔记具体信息。
## 表的设计
### 笔记本(NoteBook)
#### redis
  Key      	Value       
  LoginName	List<NoteBook>(JSON)

#### HBase
表名：NoteBook

rowkey ：  loginName_timestamp

列簇1：noteBookInfo（noteBookInfo）

列：
notebookname（notebookname）:笔记本名称

createTime（createTime）:创建时间

status（status）:状态

noteList（noteList）：笔记本下的笔记列表，是个json串（noteRowKey|name| createTime| status）  

### 笔记(Note)
#### redis
   key                 value
   笔记本的rowkey        List<String>
   List<String>: List<笔记本的rowkey|笔记本名称|创建时间|笔记状态|笔记本内容>
### HBase
表名：Note

RowKey ： loginName_timestamp

列簇1:noteInfo(noteInfo)：笔记信息

列： 

notename（noteName）：笔记的名字

createTime（createTime）：创建时间

status（status）：笔记状态    

列簇2：ContentInfo（contentInfo）：笔记本内容信息

列：

content（content）：笔记内容


