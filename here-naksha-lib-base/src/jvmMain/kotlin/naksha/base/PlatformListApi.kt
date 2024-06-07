package naksha.base

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class PlatformListApi {
    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array
    actual companion object {
        @JvmStatic
        actual fun array_get_length(array: PlatformList?): Int {
            return (array as JvmList?)?.size ?: 0
        }

        @JvmStatic
        actual fun array_set_length(array: PlatformList?, length: Int) {
            TODO("Implement array set length")
        }

        @JvmStatic
        actual fun array_clear(array: PlatformList?) {
            (array as JvmList?)?.clear()
        }

        @JvmStatic
        actual fun array_get(array: PlatformList?, i: Int): Any? {
            return (array as JvmList?)?.get(i)
        }

        @JvmStatic
        actual fun array_set(array: PlatformList?, i: Int, value: Any?): Any? {
            return (array as JvmList?)?.set(i, value)
        }

        @JvmStatic
        actual fun array_delete(array: PlatformList?, i: Int): Any? {
            return (array as JvmList?)?.removeAt(i)
        }

        @JvmStatic
        actual fun array_splice(
            array: PlatformList?,
            start: Int,
            deleteCount: Int,
            vararg add: Any?
        ): PlatformList {
            val jvmList = (array as JvmList?) ?: JvmList()
            for (i in start until deleteCount) {
                jvmList.removeAt(i)
            }
            jvmList.addAll(start, add.asList())
            return jvmList
        }

        /**
         * Compares [searchElement] to elements of the array using strict equality (the same algorithm used by the === operator).
         * NaN values are never compared as equal, so [array_index_of] always returns -1 when [searchElement] is _NaN_.
         *
         * @param searchElement element to locate in the array.
         * @param fromIndex Optional, zero-based index at which to start searching, converted to an integer.
         * - Negative index counts back from the end of the array — if -length <= fromIndex < 0, fromIndex + length is used. Note, the array is still searched from front to back in this case.
         * - If fromIndex < -length or fromIndex is omitted, 0 is used, causing the entire array to be searched.
         * - If fromIndex >= length, the array is not searched and -1 is returned.
         * @return The first index of searchElement in the array; -1 if not found.
         */
        @JvmStatic
        actual fun array_index_of(
            array: PlatformList?,
            searchElement: Any?,
            fromIndex: Int?
        ): Int {
            TODO("Not yet implemented")
        }

        @JvmStatic
        actual fun array_first_index_of(
            array: PlatformList?,
            searchElement: Any?
        ): Int {
            return (array as JvmList?)?.indexOf(searchElement) ?: -1
        }

        /**
         * Compares [searchElement] to elements of the array using strict equality (the same algorithm used by the === operator).
         * _NaN_ values are never compared as equal, so [array_last_index_of] always returns `-1` when [searchElement] is _NaN_.
         *
         * @param searchElement element to locate in the array.
         * @param fromIndex Optional, zero-based index at which to start searching backwards, converted to an integer.
         * - Negative index counts back from the end of the array — if -length <= fromIndex < 0, fromIndex + length is used.
         * - If fromIndex < -length, the array is not searched and -1 is returned. You can think of it conceptually as starting at a nonexistent position before the beginning of the array and going backwards from there. There are no array elements on the way, so searchElement is never found.
         * - If fromIndex >= length or fromIndex is omitted, length - 1 is used, causing the entire array to be searched. You can think of it conceptually as starting at a nonexistent position beyond the end of the array and going backwards from there. It eventually reaches the real end position of the array, at which point it starts searching backwards through the actual array elements.
         * @return The last index of searchElement in the array; -1 if not found.
         */
        @JvmStatic
        actual fun array_last_index_of(
            array: PlatformList?,
            searchElement: Any?,
            fromIndex: Int?
        ): Int {
            TODO("Not yet implemented")
        }

        /**
         * Returns an iterator above the values of the array.
         * @return The iterator above the values of the array.
         */
        @JvmStatic
        actual fun array_entries(array: PlatformList): PlatformIterator<Any?> {
            return JvmListIterator(array)
        }

        /**
         * Appends values to the start of the array.
         * @param elements The elements to append.
         * @return The new length of the array.
         */
        @JvmStatic
        actual fun array_unshift(vararg elements: Any?): Int {
            TODO("Not yet implemented")
        }

        /**
         * Appends values to the end of the array.
         * @param elements The elements to append.
         * @return The new length of the array.
         */
        @JvmStatic
        actual fun array_push(array: PlatformList?, vararg elements: Any?): Int {
            (array as JvmList?)?.addAll(elements)
            return array?.size ?: 0
        }

        /**
         * Removes the element at the zeroth index and shifts the values at consecutive indexes down, then returns the removed
         * value. If the length is 0, _undefined_ is returned.
         */
        @JvmStatic
        actual fun array_shift(): Any? {
            TODO("Not yet implemented")
        }

        /**
         * Removes the last element from the array and returns that value. Calling [array_pop] on an empty array, returns _undefined_.
         */
        @JvmStatic
        actual fun array_pop(): Any? {
            TODO("Not yet implemented")
        }

        /**
         * Sort the elements of this array in place and return the reference to this array, sorted. The default sort order is
         * ascending, built upon converting the elements into strings, then comparing their sequences of UTF-16 code units values.
         *
         * The time and space complexity of the sort cannot be guaranteed as it depends on the implementation.
         *
         * To sort the elements in an array without mutating the original array, use [array_to_sorted].
         * @param compareFn The (optional) function to compare; if _null_ sorting will be ascending by [toString] UTF-16 code units.
         * @return _this_.
         */
        @JvmStatic
        actual fun array_sort(compareFn: ((Any?, Any?) -> Int)?): PlatformList {
            TODO("Not yet implemented")
        }

        /**
         * This is the copying version of the [array_sort] method. It returns a new array with the elements sorted in ascending order
         * or sorting using the given compare-function.
         *
         * @param compareFn The (optional) function to compare; if _null_ sorting will be ascending by [toString] UTF-16 code units.
         * @return A copy of this array, but sorted.
         */
        @JvmStatic
        actual fun array_to_sorted(compareFn: ((Any?, Any?) -> Int)?): PlatformList {
            TODO("Not yet implemented")
        }

        @JvmStatic
        actual fun array_retain_all(array: PlatformList?, vararg keep: Any?): Boolean {
            if (array == null) return false
            return (array as JvmList).retainAll(keep)
        }
    }
}