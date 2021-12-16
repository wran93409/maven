## mybais-plus扩展应用

#### 1.主要作用
- 在项目启动过程中自动为每个实体类生成对应mapper层class。
- 提供公共Mapper作为扩展实现，统一使用CommonBaseMapper进行CRUD操作，无需再继承BaseMapper和Iservice，CommonBaseMapper已具备CRUD全部能力。
#### 2.用法说明
- EnableAutoMapper注解作为工具开关，将其加到springboot项目启动类上。
- 在需要生成对应mapper的实体类上添加BuildMapper注解。
- 引入CommonBaseMapper<T>进行进行相应CRUD操作。

#### ## 3.入门案例
项目启动处：

```
@EnableDubbo
@SpringBootApplication
@ComponentScan(basePackages = {"com.chs.mobileserver.**"})
@EnableAutoMapper(basePackages = {"com.chs.mobileserver.**"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
```
对应实体类处：

```
@Data
@BuildMapper
@TableName("com_app_config")
public class ComAppConfigDO implements Serializable {
	private static final long serialVersionUID = 1L;


	@TableId(type = IdType.AUTO)
	private Long id;

	private String gmtCreate;

	private String gmtModified;

	private Integer isDeleted;

	private String creator;
}
```
实际使用：

```
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class MapperTest {

    @Autowired
    private CommonBaseMapper<ComAppConfigDO> commonBaseMapper;

    @Test
    public void getById(){
        ComAppConfigDO comAppConfigDO =commonBaseMapper.selectById(11);
        log.info("获取comAppConfigDO属性===={}", JSON.toJSONString(comAppConfigDO));
    }

}
```
