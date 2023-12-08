import java.io.File
import java.util.*
import kotlin.math.*

object Solver1 {
    fun solve1() {
        println(File("input1.txt").readLines().sumOf {
            it.filter { c -> c.isDigit() }.let { s -> (s.first() - '0') * 10 + (s.last() - '0') }
        })
    }

    fun solve2() {
        val digitWords = arrayOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")

        println(File("input1.txt").readLines().sumOf {
            val first = (1..9).minBy { d ->
                val i1 = it.indexOf(d.toString())
                val i2 = it.indexOf(digitWords[d])
                if (i1 != -1) {
                    if (i2 != -1) {
                        min(i1, i2)
                    } else i1
                } else if (i2 != -1) i2 else 1000000000
            }

            val last = (1..9).maxBy { d ->
                max(it.lastIndexOf(d.toString()), it.lastIndexOf(digitWords[d]))
            }
            first * 10 + last
        })
    }
}

object Solver2 {
    private fun parseGames() = File("input2.txt").readLines().map { line ->
        Pair(
            line.split(':', ' ')[1].toInt(),
            line.split(": ")[1].split("; ").map { clause ->
                clause.split(", ").map {
                    val sp = it.split(" ")
                    Pair(sp[0].toInt(), sp[1])
                }
            }
        )
    }
    fun solve1() {
        val games = parseGames()
        val limits = hashMapOf("red" to 12, "green" to 13, "blue" to 14)
        val res = games.asSequence().filter { g ->
            g.second.all { l ->
                l.all {
                    it.first <= limits[it.second]!!
                }
            }
        }.sumOf { it.first }
        println(res)
    }

    fun solve2() {
        val games = parseGames()
        val res = games.sumOf { g ->
            val cubes = hashMapOf("red" to 0, "green" to 0, "blue" to 0)
            g.second.forEach { l ->
                l.forEach {
                    cubes[it.second] = max(cubes[it.second]!!, it.first)
                }
            }
            cubes["red"]!! * cubes["green"]!! * cubes["blue"]!!
        }
        println(res)
    }
}


object Solver3 {
    // solve1 missing because I changed it into solve2 and it was too late to Ctrl+Z
    fun solve2() {
        val lines = File("input3.txt").readLines()
        fun isPart(c: Char) = c != '.' && !c.isDigit()

        val parts = hashMapOf<Pair<Int, Int>, ArrayList<Int>>()

        val res = lines.indices.forEach { i ->
            val l = lines[i]
            Regex("\\d+").findAll(l).forEach { g ->
                val range = max((g.range.first - 1), 0)..min((g.range.last + 1), lines[0].length - 1)
                (
                        range.map { Pair(i - 1, it) }.asSequence() +
                                range.map { Pair(i + 1, it) } +
                                Pair(i, range.first) + Pair(i, range.last)
                        ).find { (x, y) ->
                        x in lines.indices && lines[x][y] == '*'
                    }?.let { p ->
                        parts[p] = parts.getOrDefault(p, arrayListOf()).also { it.add(g.value.toInt()) }
                    }
            }
        }

        println(parts.asSequence().filter { it.value.size == 2 }.sumOf { it.value[0] * it.value[1] })
    }
}

object Solver4 {
    private fun parseCards() = File("input4.txt").readLines().map { l ->
        val (winning, current) = l.split(':')[1].split("|")
        Pair(
            winning.split(" ").asSequence().filter { it.isNotEmpty() }.map { it.toInt() }.toSet(),
            current.split(" ").asSequence().filter { it.isNotEmpty() }.map { it.toInt() }.toList(),
        )
    }
    fun solve1() {
        val cards = parseCards()
        println(cards.sumOf { c ->
            val matches = c.second.count { c.first.contains(it) }
            if (matches == 0) 0 else 1 shl (matches - 1)
        })
    }

    fun solve2() {
        val cards = parseCards()
        val cache = hashMapOf<Int, Int>()
        fun calculateCardWins(i: Int): Int {
            if (i in cache) return cache[i]!!
            val matches = cards[i].second.count { cards[i].first.contains(it) }
            val res = 1 + ((i + 1)..min(i + matches, cards.size - 1)).sumOf { calculateCardWins(it) }
            cache[i] = res
            return res
        }
        println(cards.indices.sumOf { calculateCardWins(it) })
    }
}

object Solver5 {
    class RangeMap {
        data class Transform(val dest: Long, val source: Long, val length: Long) {
            val sourceRange: LongRange
                get() = source until (source + length)
        }

        private val transforms = ArrayList<Transform>()
        fun addRange(dest: Long, source: Long, length: Long) {
            transforms.add(Transform(dest, source, length))
        }

        operator fun get(v: Long): Long {
            for (r in transforms) {
                if (v in r.sourceRange) {
                    return v - r.source + r.dest
                }
            }
            return v
        }

        fun getRanges(l: List<LongRange>): List<LongRange> {
            val result = ArrayList<LongRange>()
            val stack = Stack<LongRange>().also { it.addAll(l) }
            loop@ while (stack.isNotEmpty()) {
                val r = stack.pop()
                for (t in transforms) {
                    val intersect = max(r.first, t.sourceRange.first)..min(r.last, t.sourceRange.last)

                    if (!intersect.isEmpty()) {
                        result.add((intersect.first - t.source + t.dest)..(intersect.last - t.source + t.dest))
                        val left = r.first until t.sourceRange.first
                        val right = (t.sourceRange.last + 1)..r.last
                        if (!left.isEmpty()) {
                            stack.push(left)
                        }
                        if (!right.isEmpty()) {
                            stack.push(right)
                        }
                        continue@loop
                    }
                }
                result.add(r)
            }
            return result
        }
    }

    fun solve1() {
        val seeds = ArrayList<Long>()
        val maps = ArrayList<RangeMap>()
        File("input5.txt").useLines { lines ->
            lines.forEach { line ->
                if (line.startsWith("seeds:")) {
                    seeds.addAll(line.substring(7).split(" ").map { it.toLong() })
                } else if (line.isEmpty()) {
                    maps.add(RangeMap())
                } else if (line[0].isDigit()) {
                    val (d, s, l) = line.split(" ").map { it.toLong() }
                    maps.last().addRange(d, s, l)
                }
            }
        }
        println(seeds.minOf {
            var k = it
            for (m in maps) {
                k = m[k]
            }
            k
        })
    }

    fun solve2() {
        val seeds = ArrayList<LongRange>()
        val maps = ArrayList<RangeMap>()
        File("input5.txt").useLines { lines ->
            lines.forEach { line ->
                if (line.startsWith("seeds:")) {
                    seeds.addAll(line.substring(7).split(" ").map { it.toLong() }
                        .foldIndexed(ArrayList()) { i, arr, v ->
                            if (i % 2 == 0) {
                                arr.add(v..v)
                            } else {
                                arr[arr.size - 1] = arr.last().first until (arr.last().first + v)
                            }
                            arr
                        })
                } else if (line.isEmpty()) {
                    maps.add(RangeMap())
                } else if (line[0].isDigit()) {
                    val (d, s, l) = line.split(" ").map { it.toLong() }
                    maps.last().addRange(d, s, l)
                }
            }
        }
        println(seeds.minOf { range ->
            var k = listOf(range)
            for (m in maps) {
                k = m.getRanges(k)
            }
            k.minOf { it.first }
        })
    }
}

object Solver6 {
    // x^2 - xt + d < 0
    // D = t^2-4(d+1)
    // x = (t +- sqrt(D)) / 2
    fun solve1() {
        val lines = File("input6.txt").readLines()
        val times = lines[0].substring(6).split(" ").filter { it.isNotEmpty() }.map { it.toInt() }
        val distances = lines[1].substring(10).split(" ").filter { it.isNotEmpty() }.map { it.toInt() }
        println(times.indices.fold(1) { a, it ->
            val d = sqrt((times[it].toDouble() * times[it]) - 4.0 * (distances[it] + 1)) / 2.0
            val b = times[it] / 2.0
            val mi = ceil(b - d)
            val ma = floor(b + d)
            a * (ma.toInt() - mi.toInt() + 1)
        })
    }

    fun solve2() {
        val lines = File("input6.txt").readLines()
        val times = listOf(lines[0].substring(6).filter { it != ' ' }.toDouble())
        val distances = listOf(lines[1].substring(10).filter { it != ' ' }.toDouble())
        println(times.indices.fold(1) { a, it ->
            val d = sqrt((times[it] * times[it]) - 4.0 * (distances[it] + 1)) / 2.0
            val b = times[it] / 2.0
            val mi = ceil(b - d)
            val ma = floor(b + d)
            a * (ma.toInt() - mi.toInt() + 1)
        })
    }
}

object Solver7 {
    val cardLabels = listOf('2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A')

    open class Hand(val cards: String): Comparable<Hand> {
        enum class Type {
            HIGH, PAIR, TWO_PAIR, THREE, FULL_HOUSE, FOUR, FIVE;
            companion object {
                fun fromCounts(first: Int, second: Int) =
                    when (first) {
                        5 -> FIVE
                        4 -> FOUR
                        3 -> when (second) {
                            2 -> FULL_HOUSE
                            1 -> THREE
                            else -> error("impossible")
                        }
                        2 -> when (second) {
                            2 -> TWO_PAIR
                            1 -> PAIR
                            else -> error("impossible")
                        }
                        1 -> HIGH
                        else -> error("impossible")
                    }
            }
        }

        open val type = run {
            val counts = hashMapOf<Char, Int>()
            cards.forEach { counts[it] = counts.getOrDefault(it, 0) + 1 }
            val nums = counts.values.sortedDescending()
            Type.fromCounts(nums[0], nums.getOrElse(1) { 0 })
        }

        override fun compareTo(other: Hand): Int {
            var res = type.compareTo(other.type)
            if (res != 0) return res
            for (i in 0..4) {
                res = cardLabels.indexOf(cards[i]).compareTo(cardLabels.indexOf(other.cards[i]))
                if (res != 0) return res
            }
            return 0
        }

        override fun toString(): String {
            return "Hand(cards='$cards', type=$type)"
        }
    }

    val cardLabelsJoker = listOf('J', '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'Q', 'K', 'A')

    class JokerHand(cards: String): Hand(cards) {
        override val type = run {
            val counts = hashMapOf<Char, Int>()
            var jokerCount = 0
            cards.forEach {
                if (it != 'J') {
                    counts[it] = counts.getOrDefault(it, 0) + 1
                } else {
                    jokerCount++
                }
            }
            val nums = counts.values.sortedDescending()
            Type.fromCounts(nums.getOrElse(0) { 0 } + jokerCount, nums.getOrElse(1) { 0 })
        }
        override fun compareTo(other: Hand): Int {
            var res = type.compareTo(other.type)
            if (res != 0) return res
            for (i in 0..4) {
                res = cardLabelsJoker.indexOf(cards[i]).compareTo(cardLabelsJoker.indexOf(other.cards[i]))
                if (res != 0) return res
            }
            return 0
        }
    }

    fun solve1() {
        val cards = File("input7.txt").readLines().map { line ->
            line.split(' ').let { Pair(Hand(it[0]), it[1].toInt()) }
        }
        println(cards.sortedBy { it.first }.mapIndexed { idx, (_, bid) ->
            bid * (idx + 1)
        }.sum())
    }

    fun solve2() {
        val cards = File("input7.txt").readLines().map { line ->
            line.split(' ').let { Pair(JokerHand(it[0]), it[1].toInt()) }
        }
        println(cards.sortedBy { it.first }.mapIndexed { idx, (_, bid) ->
            bid * (idx + 1)
        }.sum())
    }
}

object Solver8 {
    private fun readData() = File("input8.txt").readLines().let { lines ->
        Pair(lines[0], lines.asSequence().drop(2).map { line ->
            Regex("(\\w{3}) = \\((\\w{3}), (\\w{3})\\)").matchEntire(line)!!.groups.let {
                it[1]!!.value to Pair(it[2]!!.value, it[3]!!.value)
            }
        }.toMap())
    }
    fun solve1() {
        val (dirs, map) = readData()
        var i = 0
        var cur = "AAA"
        while (true) {
            cur = if (dirs[i % dirs.length] == 'L') map[cur]!!.first else map[cur]!!.second
            if (cur == "ZZZ") break
            i++
        }
        println(i+1)
    }
    fun solve2() {
        val (dirs, map) = readData()
        val destCycles = map.keys.filter { it[2] == 'A' }.map { start ->
            val met = hashMapOf<Pair<String, Int>, Int>()
            var cur = start
            var i = 0
            met[Pair(start, 0)] = 0
            while (true) {
                cur = if (dirs[i % dirs.length] == 'L') map[cur]!!.first else map[cur]!!.second
                i++
                val key = Pair(cur, i % dirs.length)
                if (met.containsKey(key)) {
                    val prevOffset = met[key]!!
                    return@map (i - prevOffset).toLong()
                    /* should be this but for some reason pair.second == listOf(pair.first)
                        Pair(
                            i - prevOffset,
                            met.entries.asSequence().filter { it.key.first[2] == 'Z' }.map { it.value }.toList()
                        )
                    */
                } else {
                    met[key] = i
                }
            }
            // makes the compiler understand that .map returns Int
            @Suppress("UNREACHABLE_CODE")
            error("Impossible")
        }
        val res = destCycles.reduce { sa, sb ->
            var a = sa
            var b = sb
            while (b > 0) {
                val temp = b
                b = a % b
                a = temp
            }
            sa * sb / a
        }
        println(res)
    }
}

fun main() {
    Solver8.solve2()
}

