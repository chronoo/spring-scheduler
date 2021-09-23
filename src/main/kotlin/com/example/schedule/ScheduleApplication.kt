package com.example.schedule

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.support.CronTrigger
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import java.util.concurrent.ScheduledFuture

@SpringBootApplication
class ScheduleApplication

fun main(args: Array<String>) {
    runApplication<ScheduleApplication>(*args)
}

@Configuration
class SchedulerConfig {
    @Bean
    fun customScheduler(): TaskScheduler =
        ThreadPoolTaskScheduler().apply {
            setThreadNamePrefix("pool")
            poolSize = 1
            initialize()
        }
}

@Service
class ScheduleService(
    @Qualifier("customScheduler")
    val scheduler: TaskScheduler
) {
    private val jobs: MutableMap<String, ScheduledFuture<*>> = mutableMapOf()

    fun addSchedule(name: String, period: Long, task: () -> Unit) {
        scheduler.schedule(Runnable { task() }, CronTrigger("0/$period * * * * ?"))
            ?.let { jobs[name] = it }
            ?: throw IllegalStateException("schedule is failed")
    }

    fun delete(name: String) =
        jobs[name]?.let {
            it.cancel(true)
            jobs.remove(name)
        }
}

@RestController
class ScheduleController(
    private val service: ScheduleService
) {
    @PostMapping
    fun add(@RequestParam period: Long, @RequestParam name: String, @RequestParam word: String) {
        service.addSchedule(name, period) { println(word) }

    }

    @DeleteMapping("/{name}")
    fun delete(@PathVariable name: String) {
        service.delete(name)
    }
}
