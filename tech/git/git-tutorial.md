# git基础部分
<!-- TODO: git 冲突解决方法 -->
<!-- TODO: git 分支管理 -->

## git仓库的初始化
- 创建本地仓库

  在要创建仓库的目录下执行
  ```bash
  git init
  ```
- 从现有仓库克隆
  ```bash
  git clone <url>
  ```

- 从远程仓库克隆(自定义本地仓库名)
  ```bash
  git clone <url> <local repository name>
  ```

## 记录每次更新到仓库
当在仓库中，对文件进行修改时，文件有两种状态`已跟踪`和`未跟踪`。已跟踪的文件是指纳入了版本控制的文件，上次快照中有它们的记录。  

已跟踪的文件也有两种状态：`已修改`和`未修改`。除了已跟踪的文件，剩下的文件都是未跟踪状态。(新创建的文件)。  


## git分支的创建与合并
### git的几种合并策略
1. Fast-forward
   
   <div align = "center">
   <img src="../img/git-ff1.png" align=center width="400"/>  
   </div>  

   当合并没有分叉分支时，如图，git只需要将master指向最后一个提交节点上。  

   <div align = "center">
   <img src="../img/git-ff2.jpg" align=center width="400"/>  
   </div>  

   Fast-forward是git在合并两个没有分叉的分支时的默认策略，可以使用`git merge --no-ff`取消
   
2. Recursive  
   
   Recursive是git分支合并策略中最常用的策略。git在合并两个有分叉的分支时，Rescursive是默认合并策略。  

   其算法可以简述为：递归寻找路径最短的唯一共同祖先节点，然后以其为base家电进行递归三向合并。  
3. Ours
4. Octopus

