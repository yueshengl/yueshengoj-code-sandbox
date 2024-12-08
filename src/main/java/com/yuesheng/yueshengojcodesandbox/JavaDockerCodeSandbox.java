package com.yuesheng.yueshengojcodesandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.yuesheng.yueshengojcodesandbox.model.ExecuteCodeRequest;
import com.yuesheng.yueshengojcodesandbox.model.ExecuteCodeResponse;
import com.yuesheng.yueshengojcodesandbox.model.ExecuteMessage;
import com.yuesheng.yueshengojcodesandbox.model.JudgeInfo;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Java Docker代码沙箱实现（重写部分模板方法）
 */
@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {


    private static final long TIME_OUT = 5000L;//超时时间


    //缓存镜像信息
    private static final Set<String> pulledImages = new HashSet<>();
    private static final String IMAGE_NAME = "openjdk:8-alpine";

    public static void initializeDockerImage() {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        List<Image> images = dockerClient.listImagesCmd().exec();
        boolean imageExists = images.stream()
                .anyMatch(image -> Arrays.asList(image.getRepoTags()).contains(IMAGE_NAME));

        if (!imageExists) {
            synchronized (JavaDockerCodeSandbox.class) {
                if (!pulledImages.contains(IMAGE_NAME)) {
                    PullImageCmd pullImageCmd = dockerClient.pullImageCmd(IMAGE_NAME);
                    try {
                        pullImageCmd.exec(new PullImageResultCallback()).awaitCompletion();
                        pulledImages.add(IMAGE_NAME);
                        System.out.println("镜像已成功拉取：" + IMAGE_NAME);
                    } catch (InterruptedException e) {
                        System.out.println("拉取镜像失败");
                        throw new RuntimeException(e);
                    }
                }
            }
        } else {
            System.out.println("镜像已存在：" + IMAGE_NAME);
        }
    }


    /**
     * 3.创建容器，上传编译文件
     * @param userCodeFile
     * @param inputList
     * @return
     */
    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        //（4  权限管理，Docker容器已经做了系统层面的隔离，比较安全，但不能保证绝对安全）
        //获取默认的Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        //拉取镜像
        String image = "openjdk:8-alpine";

        //创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        //2  限制内存100MB
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        //hostConfig.withSecurityOpts(Arrays.asList("seccomp=安全管理配置字符串"));//4  权限管理
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)   //3  限制网络资源，设置网络配置为关闭
                .withReadonlyRootfs(true)   //4  权限管理,限制用户不能向root根目录写文件
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)  //便于交互,接受输入
                .exec();
        //获取容器ID
        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();
        //启动容器
        dockerClient.startContainerCmd(containerId).exec();

        //docker exec naughty_euclid java -cp /app Main 1 2
        //创建、执行命令并获取结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            StopWatch stopWatch = new StopWatch();
            String[] inputArgsArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("创建执行命令：" + execCreateCmdResponse);
            String exexId = execCreateCmdResponse.getId();


            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] message = {null};
            final String[] errorMessage = {null};
            long time = 0L;
            //1  超时控制
            final boolean[] timeOut = {false};
            //执行命令结果回调
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {

                @Override
                public void onComplete() {
                    //如果执行完成，则表示没有超时
                    timeOut[0] = false;
                    super.onComplete();
                    dockerClient.stopContainerCmd(containerId).exec();//停止容器运行
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            final long[] maxMemory = {0L};
            //获取占用的内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            //统计数据结果回调
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(maxMemory[0], statistics.getMemoryStats().getUsage());
                }
                @Override
                public void onStart(Closeable closeable) {
                }
                @Override
                public void onError(Throwable throwable) {
                }
                @Override
                public void onComplete() {
                }
                @Override
                public void close() {
                }
            });
            statsCmd.exec(statisticsResultCallback);

            //正式开始执行运行命令
            try {
                stopWatch.start();
                dockerClient.execStartCmd(exexId).exec(execStartResultCallback).awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
                statsCmd.close();//停止统计内存数据
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }
        dockerClient.stopContainerCmd(containerId).exec();
        dockerClient.removeContainerCmd(containerId).exec();
        return executeMessageList;
    }

    @Override
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        //取用时最大值，便于判断是否超时
        long maxTime = 0;
        long maxMemeory = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                //用户提交的代码在执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
            Long memory = executeMessage.getMemory();
            if (memory != null) {
                maxMemeory = Math.max(maxMemeory, memory);
            }
        }
        executeCodeResponse.setOutputList(outputList);
        //正常运行完成
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        JudgeInfo judgeInfo = new JudgeInfo();
        System.out.println("转换之前的内存:"+maxMemeory);
        System.out.println("转换之后的内存:"+maxMemeory/1024);
        judgeInfo.setMemory(maxMemeory/1024);//转换为KB单位
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }
}

