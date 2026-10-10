package ru.alox1d.wordnote.huawei.domain.services.dateparser

import kotlin.math.absoluteValue

class DamerauLevenshtein(var str1: String, var str2: String, var distance: Double = 0.0) {
    //    #str1;
//    #str2;
    var matrix: Array<Array<Double?>?> = arrayOfNulls(str1.length+1);
    var original_string:String = ""
    var target_string:String = ""
//Array(str1.length, { DoubleArray() })

//    var strings:ArrayList<String>
//    set()
    /**
     * @param {Array.<string>} strings
     */
    fun setStrings(str1: String, str2: String) {
        this.str1 = str1
        this.str2 = str2
        this.matrix = arrayOfNulls(str1.length+1);
        for (i in str1.indices) {
            this.matrix[i] = arrayOfNulls(str2.length+1)
        }
        this.calculate();
    }
    /**
     * Calculates Damerau-Levenshtein distance of the two strings.
     * @param {String} str1
     * @param {String} str2
     * @property distance
     * @property matrix
     * @function toString() returns distance calculation matrix in tile grid.
     * @function valueOf() returns this.distance
     */
    init {
        this.matrix = arrayOfNulls(str1.length+1);
        for (i in 0..str1.length) {
            this.matrix[i] = Array(str2.length+1){null}
        }
        this.calculate();
    }

    /**
     * @returns {Array.<Array.<number>>} calculation matrix
     */
//    fun getMatrix(): Array<Array<Double>> {
//        return this.matrix;
//    }

    /**
     * @returns {number} distance
     */
    fun valueOf(): Double {
        return this.distance;
    }

    /**
     * Returns string that shows Damerau-Levenshtein distance calculations matrix in tile grid.
     * @returns {string} string
     */
    override fun toString(): String {
        val str1: String = ' ' + this.str1
        val str2 = ' ' + this.str2
        var str = "".padStart(4, ' ');
        for (c in str2) {
            str += " |" + c.toString().padStart(4 - 2, ' ');
        }
        str += "\r\n";
        str += "".padStart(4 + 1, '—') + "".padLeft(str2.length * 4, "+".padEnd(4, '-')) + "\r\n";
        for (i in this.matrix.indices) {
            str += str1[i].toString().padStart(4, ' ');
            for (j in this.matrix[i]!!.indices) {
                str += " |" + this.matrix[i]?.get(j).toString().padStart(4 - 2, ' ');
            }
            str += "\r\n";
            str += "".padStart(4 + 1, '—') + "".padLeft(str2.length * 4, "+".padEnd(4, '–')) + "\r\n";
        }
        return str.substring(0, str.length - 2);
    }

    fun calculate() {
        for (i in 0..this.str1.length) {
            for (j in 0..this.str2.length) {
                this.matrix[i]?.set(j, D(i, j));
            }
        }
        this.distance = this.matrix[this.str1.length]!!.get(this.str2.length)!!
    }

//    fun toMatrix(i: Int, j: Int): Double {
////        if (typeof (this.#matrix[i][j]) == 'undefined') {
//        if (i>= this.matrix.size && j>=this.matrix[i]!!.size) {
//            this.matrix [i]?.set(j, this.D(i, j));
//        }
//        return this.matrix[i]!!.get(j)!!;
//    }
fun toMatrix(i: Int, j: Int): Double {
//        if (typeof (this.#matrix[i][j]) == 'undefined') {
    if (this.matrix[i]!!.get(j) == null){
        this.matrix[i]!!.set(j, this.D(i, j));
    }

    return this.matrix[i]!![j]!!;
}

    fun D(i: Int, j: Int): Double {
        if (i == 0 && j == 0) {
            return 0.0;
        } else {
            var cost = 0

            if ((i-1<0) xor (j-1<0)){
                cost = 1
            } else if ((i-1<0) && (j-1<0)) {
                cost = 0
            } else {
                cost = (this.str1[i - 1] != this.str2[j - 1]).toInt();
            }
            val values = ArrayList<Double>();
            if (i > 0) {
                values.add(this.toMatrix(i - 1, j) + 1);
            }
            if (j > 0) {
                values.add(this.toMatrix(i, j - 1) + 1);
            }
            if (i > 0 && j > 0) {
                values.add(this.toMatrix(i - 1, j - 1) + cost);
            }
            if (i > 1 && j > 1 && this.str1 [i - 1] == this.str2 [j - 2] && this.str1 [i - 2] == this.str2 [j - 1]) {
                values.add(this.toMatrix(i - 2, j - 2) + 1);
            }
//            return Math.min.apply (null, values);
            return values.minByOrNull {it}!!
        }
    }
}

/**
 * Calculate levenshtein distance between two strings.
 * @param {String} str1
 * @param {String} str2
 * @return {number} the levenshtein distance between two strings (>=0).
 */
fun distance(str1:String, str2:String):Double {
    return DamerauLevenshtein(str1, str2).distance;
}

/**
 * Selects the most similar string to original_string from target_strings.
 * @param {String} original_string
 * @param {Array.<String>} target_strings
 * @returns {{closest_string: String, distance: Number}} closest string and distance to it.
 */
fun closest(original_string:String, target_strings:ArrayList<String>) :Result{
    val calculator = DamerauLevenshtein(original_string, target_strings[0]);
    val result:Result = Result(target_strings[0], calculator.distance)
    for (i in 1 until target_strings.size) {
        val string = target_strings[i]
        calculator.original_string = original_string
        calculator.target_string = string
        val distanceLocal = calculator.distance;
        if (distanceLocal < result.distance) {
            result.closest_string = string;
            result.distance = distanceLocal;
        }
    }
    return result;
}
class Result(var closest_string:String, var distance:Double)
