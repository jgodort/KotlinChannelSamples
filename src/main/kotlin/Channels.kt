import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

@ExperimentalCoroutinesApi
fun main() {
    //channelBasics()
    //closingAndIterationOverChannels()
    //buildingChannelProducers()
    //pipelines()
    //primeNumberPipelines()
    //fanOut()
    //fanIn()
    //bufferedChannels()
    //pingPong()
    ticker()
}


private fun channelBasics() = runBlocking {
    val channel = Channel<Int>()
    launch {
        for (x in 1..5) {
            channel.send(x * x)
        }
    }

    repeat(5) {
        println(channel.receive())
    }
    println("Done")
}

private fun closingAndIterationOverChannels() = runBlocking {
    val channel = Channel<Int>()
    launch {
        for (x in 1..5) {
            channel.send(x * x)
        }
        channel.close()
    }
    for (y in channel) {
        println(y)
    }
    println("Done!")
}

@ExperimentalCoroutinesApi
fun CoroutineScope.produceSquares(): ReceiveChannel<Int> = produce {
    for (x in 1..5) {
        send(x * x)
    }
}

@ExperimentalCoroutinesApi
private fun buildingChannelProducers() = runBlocking {
    val squares = this.produceSquares()
    squares.consumeEach {
        println(it)
    }
    println("Done!")
}

@ExperimentalCoroutinesApi
fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1
    while (true) send(x++)
}

@ExperimentalCoroutinesApi
fun CoroutineScope.square(numbers: ReceiveChannel<Int>): ReceiveChannel<Int> = produce {
    for (x in numbers) send(x * x)
}

private fun pipelines() = runBlocking {

    val numbers = produceNumbers()
    val squares = square(numbers)
    for (i in 1..5) {
        println(squares.receive())
    }
    println("Done!")
    coroutineContext.cancelChildren()
}

@ExperimentalCoroutinesApi
fun CoroutineScope.numbersFrom(start: Int) = produce {
    var x = start
    while (true) send(x++) // infinite stream of integers from start
}

@ExperimentalCoroutinesApi
fun CoroutineScope.filter(numbers: ReceiveChannel<Int>, prime: Int) = produce<Int> {
    for (x in numbers) if (x % prime != 0) send(x)
}

private fun primeNumberPipelines() = runBlocking {
    var cur = numbersFrom(2)
    for (i in 1..10) {
        val prime = cur.receive()
        println(prime)
        cur = filter(cur, prime)
    }
    coroutineContext.cancelChildren()
}

fun CoroutineScope.produceNumbers2() = produce {
    var x = 1
    while (true) {
        send(x++)
        delay(100)
    }
}

fun CoroutineScope.launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
    for (msg in channel) {
        println("Processor #$id received #$msg")
    }
}

private fun fanOut() = runBlocking {
    val producer = produceNumbers2()
    repeat(5) {
        launchProcessor(it, producer)
    }
    delay(950)
    producer.cancel()
}

private suspend fun sendString(channel: SendChannel<String>, s: String, time: Long) {
    while (true) {
        delay(time)
        channel.send(s)
    }
}

private fun fanIn() = runBlocking {
val channel=Channel<String>()
    launch { sendString(channel, "foo",200L) }
    launch { sendString(channel,"BAR!",500L) }
    repeat(6){
        println(channel.receive())
    }
    coroutineContext.cancelChildren()
}

private fun bufferedChannels()= runBlocking {
    val channel= Channel<Int>(4)
    val sender=launch {
        repeat(10){
            println("Sending $it")
            channel.send(it)
        }
    }
    delay(1000)
    sender.cancel()
}
data class Ball(var hits:Int)

private suspend fun player(name:String,table:Channel<Ball>){
    for (ball in table){
        ball.hits++
        println("$name $ball")
        delay(300)
        table.send(ball)
    }
}

private fun pingPong()= runBlocking {
    val table= Channel<Ball>()
    launch { player("ping",table) }
    launch { player("pong",table) }
    table.send(Ball(0))
    delay(1000)
    coroutineContext.cancelChildren()
}


private fun ticker()= runBlocking {
    val tickerChannel= ticker(delayMillis = 100,initialDelayMillis = 0)
    var nextElement= withTimeoutOrNull(1){
        tickerChannel.receive()
    }
    println("Initial element is available immediately: $nextElement")

    nextElement= withTimeoutOrNull(50){
        tickerChannel.receive()
    }
    println("Next element is not ready in 50 ms: $nextElement")

    nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
    println("Next element is ready in 100 ms: $nextElement")

    println("Consumer pauses for 150ms")
    delay(150)

    nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
    println("Next element is available immediately after large consumer delay: $nextElement")

    nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
    println("Next element is ready in 50ms after consumer pause in 150ms: $nextElement")

    tickerChannel.cancel()

}

