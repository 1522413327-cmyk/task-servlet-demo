# Spring IoC 整合过程记录（task-servlet-demo）

> 本文件记录本项目引入 Spring IoC 解耦的**完整交互过程**：从“该在 resources 建什么文件”，到“是否要配 web.xml 监听器”，再到“war 包如何更新”，最后“部署验证成功”。
> 可作为软件测试大赛（开发者测试方向）的**答辩素材**：清晰展示“发现问题 → 引入框架 → 标准整合 → 验证”的闭环。

---

## 一、背景与目标

项目原本采用经典三层架构（Controller → Buss → Dal），虽然接口（ITaskBuss / ITaskDal）已经定义，但**实现类是被硬编码 `new` 出来的**，层与层之间紧耦合：

- `TaskController` 中 5 处 `new TaskBuss()`
- `TaskBuss` 中每个方法都 `new TaskDal()`

**目标**：用 Spring IoC 容器 + bean 把这些依赖交给容器注入，消除硬编码耦合（即“控制反转 / 依赖注入 DI”）。

前提：已在 `pom.xml` 加入 `spring-context`（版本 `7.1.0-M2`，里程碑版，后文建议换稳定版 `6.2.x`）。

---

## 二、过程交互记录

### 第 1 轮：resources 文件夹里要新建什么文件？

**用户问**：我刚加入 spring-context 的 jar 包，要用 Spring IoC 和 bean 解决类之间耦合，在 resources 文件夹中新建什么文件？

**分析与结论**：
- 需要在 `src/main/resources/` 下新建 **`applicationContext.xml`**（Spring 基于 XML 配置时的核心文件，惯例命名，也可用 `beans.xml` / `spring-config.xml`）。
- 它负责：① 把 `TaskDal`、`TaskBuss` 注册为 Spring bean；② 声明它们之间的依赖（把 `taskDal` 注入给 `taskBuss`）。

**落地改动**：

1. `applicationContext.xml`（新建，声明 bean + setter 注入）：
```xml
<bean id="taskDal" class="cn.edu.mju.band.dal.impl.TaskDal"/>

<bean id="taskBuss" class="cn.edu.mju.band.buss.impl.TaskBuss">
    <property name="taskDal" ref="taskDal"/>
</bean>
```

2. `TaskBuss.java`（去掉内部 `new TaskDal()`，改为持有接口 + setter 注入点，纯 POJO）：
```java
private ITaskDal taskDal;

public void setTaskDal(ITaskDal taskDal) {
    this.taskDal = taskDal;
}
// 方法体内统一用 taskDal.xxx()，不再 new TaskDal()
```

3. `TaskController.java`（去掉 5 处 `new TaskBuss()`，改为成员变量 + `init()` 中加载容器）：
```java
private ITaskBuss buss;

@Override
public void init() throws ServletException {
    ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
    this.buss = ctx.getBean("taskBuss", ITaskBuss.class);
}
```

**验证**：`mvn compile` 通过（EXIT=0）。

> 此时采用的是**轻量级整合**：Servlet 自己在 `init()` 里 `new` 容器。能跑，但容器不随 Web 应用优雅关闭、也不在组件间共享。

---

### 第 2 轮：改造后不是需要在 web.xml 加入“监视器”吗？

**用户问**：你改造后不是需要在 web.xml 加入监视器吗？

**澄清（两种整合方式）**：
- 轻量写法（`init()` 自己加载）**不强制**配监听器也能运行；
- 但**教科书 / 标准 Spring + Web 整合**应当配 `ContextLoaderListener`（用户所说的“监视器”），让容器挂在 `ServletContext` 上、随应用启停、全局共享一份。

**前提**：`ContextLoaderListener` 在 **`spring-web`** 模块（不在 `spring-context`），必须补该依赖。

**落地改动**：

1. `pom.xml` 新增 `spring-web` 依赖（与 spring-context 同版）：
```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
    <version>7.1.0-M2</version>
    <scope>compile</scope>
</dependency>
```

2. `web.xml` 由老旧的 **Servlet 2.3 DTD** 升级为 **Jakarta Web 6.0 schema**，并加入监听器与配置文件位置：
```xml
<context-param>
    <param-name>contextConfigLocation</param-name>
    <param-value>classpath:applicationContext.xml</param-value>
</context-param>

<listener>
    <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
</listener>
```

3. `TaskController.java` 改为从 `ServletContext` 取容器（不再自己 `new`）：
```java
@Override
public void init() throws ServletException {
    ApplicationContext ctx =
        WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
    this.buss = ctx.getBean("taskBuss", ITaskBuss.class);
}
```

**监听器的作用（标准整合）**：
```
Web 应用启动
 └─ ContextLoaderListener 读 contextConfigLocation
      ├─ 创建 Spring 容器（ROOT WebApplicationContext）
      └─ 存入 ServletContext（全局共享）
TaskController.init() ── WebApplicationContextUtils 取出容器 ── getBean("taskBuss")
Web 应用停止 ── ContextLoaderListener 关闭容器、释放资源
```

**验证**：`mvn compile` 通过（EXIT=0）。

---

### 第 3 轮：war 包要怎么更新？

**排查发现（关键）**：
- 占用 8080 的进程是 **IntelliJ IDEA 2025.2 的 Tomcat 运行实例**（命令行含 `IntelliJIdea2025.2\tomcat` 与 `catalina.home=D:\Tomcat 11.0`），并非手动启动的 Tomcat 服务；
- `Tomcat11` Windows 服务本身为 **Stopped**；
- `D:\Tomcat 11.0\webapps` 下并存 `task-servlet-demo`、`task-servlet-demo.war`、`task_servlet_demo_war` 三份（IDEA 部署与手动放 war 混用导致路径混乱）。

**结论**：项目实际通过 **IDEA 的 Tomcat 运行配置**部署，应走 IDEA 重新部署，而非手动拷 war。

**推荐更新方式（IDEA 重新部署）**：
1. Maven 面板执行 `package`（已生成最新 war：`target/task-servlet-demo.war`，11MB，含 Spring 依赖）；
2. IDEA `Run/Services` 窗口找到 Tomcat 配置 → 点 **Redeploy**（重新部署）或 **Restart**；
3. 按控制台输出的 `Application context` 访问（如 `http://localhost:8080/<context>/task`）。

**备选方式（手动 war + 服务，不推荐混用）**：
1. 先停 IDEA 的 Tomcat（释放 8080，否则新实例 `BindException`）；
2. 清理 `webapps` 下重复目录（`task-servlet-demo`、`task-servlet-demo.war`、`task_servlet_demo_war`）；
3. 复制新 war 到 `webapps`；
4. 启动 `Tomcat11` 服务；
5. 访问 `http://localhost:8080/task-servlet-demo/task`。

---

### 第 4 轮：部署验证成功

**用户截图确认**：`http://localhost:8080/task-servlet-demo/task` 正常渲染，列表显示数据（id=6，创建时间 2026-10-14）。

**截图证明的事**：
| 证据 | 说明 |
|---|---|
| 地址能访问，上下文 `/task-servlet-demo` | 部署更新成功、路径统一 |
| 列表有数据（今天新建的记录） | 数据库连接已通 + Spring IoC 注入生效（Controller→Buss→Dal 全链路经容器） |
| 优先级显示“中”、状态“进行中” | `TaskFormatter` 中文映射正常 |
| 截止日期为空但不崩 | 之前重构修复的 `dueDate` 空值 NPE 保护生效 |
| 有“删除/修改”链接 | 增删改查入口完整 |

---

## 三、最终改动清单（文件汇总）

| 文件 | 改动类型 | 要点 |
|---|---|---|
| `src/main/resources/applicationContext.xml` | 新建/填写 | 声明 `taskDal`、`taskBuss` 两个 bean，`<property>` 注入 |
| `src/main/java/.../buss/impl/TaskBuss.java` | 改造 | 去 `new TaskDal()`，加 `ITaskDal taskDal` + `setTaskDal()` |
| `src/main/java/.../controller/TaskController.java` | 改造 | 去 `new TaskBuss()`，加 `buss` 成员 + `init()` 取容器 |
| `src/main/webapp/WEB-INF/web.xml` | 升级 | Servlet 2.3 DTD → Jakarta Web 6.0；加 `context-param` + `ContextLoaderListener` |
| `pom.xml` | 补依赖 | 新增 `spring-web`（承载 `ContextLoaderListener` / `WebApplicationContextUtils`） |

---

## 四、解耦前后对比

```
改造前：Controller ──new──▶ TaskBuss ──new──▶ TaskDal   （硬编码实现类）
改造后：Controller ──注入──▶ ITaskBuss ◀──Spring 注入── ITaskDal ◀──注入── TaskDal
```

**收益**：若要替换 `TaskDal` 实现（如加缓存、换数据源），**只改 `applicationContext.xml` 一行 `ref`，不碰任何 Java 代码**——这就是 IoC/DI 解耦的意义。

---

## 五、验证结论

- 编译：`mvn compile` / `mvn package -DskipTests` 均通过；
- 运行：页面正常、CRUD 链路经 Spring 容器注入、数据库连接正常；
- 规范：采用标准 Spring Web 整合（`ContextLoaderListener` + Jakarta Web 6.0 schema）。

---

## 六、遗留 / 可选事项

1. **Spring 版本**：`7.1.0-M2` 为里程碑预览版，建议换稳定版 `6.2.x`（仅改 `pom.xml` 两个版本号，代码无需动）。
2. **数据库密码**：`db.properties` 中账号密码需与本地 MySQL 一致（已验证当前可连通）。
3. **单元测试**：可补 JUnit 测试覆盖 DAO / Controller 边界与注入用例（备赛加分项）。
4. **进阶（可选）**：若想更“Spring 风格”，可进一步用 `@Autowired` + 注解扫描替代 XML 中 `getBean` 按字符串 id 获取。
