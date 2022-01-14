package com.example.blelocker

interface UseCase {
    interface Execute<I, O> {
        operator fun invoke(input: I): O
    }
}