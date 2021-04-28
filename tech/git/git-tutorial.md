# git基础部分



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

## 查看提交历史  

用以下命令查询git提交的历史记录  
```bash
git log
``` 

- 参数
  - `-p`或者`--path`,可选参数 <-2>  
    显示每次提交所引入的差异，<-2>用于限制只显示最近两次提交。（在命令中没有尖括号）  。

  - --stat  
    只显示简略统计信息  

  - --pretty  
    使用不同于默认格式的方式显示提交历史,oneline则将每个提交放在一行显示
    ```bash
    $ git log --pretty=oneline
    ca82a6dff817ec66f44342007202690a93763949 changed the version number
    085bb3bcb608e1e8451d4b2432f8ecbe6306e7e7 removed unnecessary test
    a11bef06a3f659402fe7563abf99ad00de2209e6 first commit
    ```
    - format  
        ```bash
        $ git log --pretty=format:"%h - %an, %ar : %s"
        ca82a6d - Scott Chacon, 6 years ago : changed the version number
        085bb3b - Scott Chacon, 6 years ago : removed unnecessary test
        a11bef0 - Scott Chacon, 6 years ago : first commit  
        ```  
    git log --pretty=format常用选项   

    |选项|说明|
    |---|---|
    |%H|提交的完成的hash值|
    |%h|提交的简写的哈希值|
    |%T|树的完整哈希值|
    |%t|树的简写哈希值|
    |%P|父提交的完整哈希值|
    |%p|父提交的简写哈希值|
    |%an|作者名字|
    |%ae|作者的电子邮箱|
    |%ad|作者修订日期(可以用 --date=<format> 来定制格式)|
    |%ar|作者修订日期，按多久以前的方式显示|
    |%cn|提交者的名字|
    |%ce|提交者的电子邮箱|
    |%cd|提交日期|
    |%cr|提交日期（按距今时常显示）|
    |%s|提交说明|

## 撤销操作  
- 提交后发现有缺漏,重新提交：
  ```bash
  git add forgotten_file  
  git commit --amend
  ```  
  将缓冲区中的文件提交，并覆盖原提交信息

- 取消暂存的文件:  
  ```bash
  git reset HEAD <file>
  ```  

- 撤销对文件的修改
  
  ```bash
  git checkout -- <file>
  ```
## 远程仓库
- 查看远程仓库
  ```bash
  git remote add origin
  ```
  - 显示URL
    ```bash
    git remote -v
    ```
- 添加远程仓库
  ```bash
  git remote add <shortname> <url>
  ```

- 从远程仓库拉取信息（远程仓库上有，本地没有）  
  ```bash
  git fetch <remote>
  ```  
  fetch命令只会将数据下载到本地，不会自动合并或修改当前工作。需要手动合并到当前的本地分支中。  

  若设置了跟踪远程分支，则可以用`git pull`命令来自动抓取后合远程分支到本地。   

  默认情况下`git clone`命令可以自动设置本地master分支跟踪克隆的远程仓库的master分支，`运行git pull 通常会从最初克隆的服务器上抓取数据并自动尝试合并到当前所在的分支。`
- 推送到远程仓库  
  ```bash
  git push <remote> <brach>
  ```  

  当在push之前，有其他人将提交push到该分支上时，则直接push会失败，需要fetch下来，合并之后再提交。

- 查看某个远程分支  
  
  ```bash
  git remote show <remote repository>
  ```

- 重命名远程仓库
  
  ```bash
  git remote rename <old name> <new name>
  ```  

## 打标签
- 列出标签
  ```bash
  git tag (-l/--list)
  ```  
  条件筛选  
  ```bash
  git tag -l "v1.8.5*"
  v1.8.5
  v1.8.5-rc0
  v1.8.5-rc1
  v1.8.5-rc2
  v1.8.5-rc3
  v1.8.5.1
  v1.8.5.2
  v1.8.5.3
  v1.8.5.4
  v1.8.5.5
  ```  

- 创建标签
  - 轻量标签  
    
    不会改变的分支——只是某个特定提交的引用。  

    本质上是将校验和存储到一个文件中——没有保存任何其他信息。创建清凉标签不要使用-a, -s 或者 -m选项，只需要提供标签名字。
    ```bash
    git tag <tag_name>
    ``` 

    若在轻量标签上使用git show,则不会看到任何额外的标签信息，如下所示

    ```bash
    $ git show v1.4-lw
    commit ca82a6dff817ec66f44342007202690a93763949
    Author: Scott Chacon <schacon@gee-mail.com>
    Date:   Mon Mar 17 21:52:11 2008 -0700

        changed the version number
    ```  

  - 附注标签  
    
    在创建标签的时候使用tag命令时，指定-a选项:

    ```bash
    $ git tag -a v1.4 -m "my version 1.4"
    $ git tag
    v0.1
    v1.3
    v1.4
    ```
    -m选项指定了一条将会存储在标签中的信息。
## git分支的创建与合并
<!-- TODO: git 分支管理 -->
### git的几种合并策略
<!-- TODO: git 冲突解决方法 -->
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

