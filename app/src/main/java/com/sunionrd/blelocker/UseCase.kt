package com.sunionrd.blelocker

interface UseCase {
    interface Execute<I, O> {
        operator fun invoke(input: I): O
    }
}