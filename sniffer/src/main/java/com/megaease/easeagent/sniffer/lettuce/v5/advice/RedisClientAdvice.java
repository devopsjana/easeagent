package com.megaease.easeagent.sniffer.lettuce.v5.advice;

import com.megaease.easeagent.common.ForwardLock;
import com.megaease.easeagent.core.AdviceTo;
import com.megaease.easeagent.core.Definition;
import com.megaease.easeagent.core.Injection;
import com.megaease.easeagent.core.Transformation;
import com.megaease.easeagent.core.interceptor.AgentInterceptorChain;
import com.megaease.easeagent.core.interceptor.AgentInterceptorChainInvoker;
import com.megaease.easeagent.sniffer.AbstractAdvice;
import com.megaease.easeagent.sniffer.Provider;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

@Injection.Provider(Provider.class)
public abstract class RedisClientAdvice implements Transformation {

    @Override
    public <T extends Definition> T define(Definition<T> def) {
        return def.type(hasSuperType(named("io.lettuce.core.RedisClient"))
                .or(named("io.lettuce.core.RedisClient"))
        )
//                .transform(connect(nameStartsWith("connect")
//                        .and(returns(named("io.lettuce.core.api.StatefulRedisConnection")))
//                        .and(isPublic())))
//                .transform(connectAsync(nameStartsWith("connect").and(nameEndsWith("Async"))
//                        .and(returns(named("io.lettuce.core.ConnectionFuture").or(named("java.util.concurrent.CompletableFuture"))))
//                        .and(isPrivate())))
                .transform(connectAsync((named("connectStandaloneAsync")
                                .or(named("connectPubSubAsync"))
                                .or(named("connectSentinelAsync"))).and(isPrivate())
                        )
                )
                .end()
                ;
    }

    @AdviceTo(ConnectStatefulASync.class)
    public abstract Definition.Transformer connectAsync(ElementMatcher<? super MethodDescription> matcher);

    static class ConnectStatefulASync extends AbstractAdvice {
        @Injection.Autowire
        public ConnectStatefulASync(@Injection.Qualifier("builder4RedisClientConnectAsync") AgentInterceptorChain.Builder builder,
                                    AgentInterceptorChainInvoker agentInterceptorChainInvoker
        ) {
            super(builder, agentInterceptorChainInvoker);
        }

        @Advice.OnMethodEnter
        public ForwardLock.Release<Map<Object, Object>> enter(
                @Advice.This Object invoker,
                @Advice.Origin("#m") String method,
                @Advice.AllArguments Object[] args
        ) {
            return this.doEnter(invoker, method, args);
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public Object exit(@Advice.Enter ForwardLock.Release<Map<Object, Object>> release,
                           @Advice.This Object invoker,
                           @Advice.Origin("#m") String method,
                           @Advice.AllArguments Object[] args,
                           @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object retValue,
                           @Advice.Thrown Throwable throwable
        ) {
            return this.doExit(release, invoker, method, args, retValue, throwable);
        }
    }
}
