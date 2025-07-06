"use client"

import type React from "react"

import { useState, useRef, useEffect } from "react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { SearchSuggestions } from "./search-suggestions"
import { useSearchSuggestions } from "@/hooks/use-search-suggestions"
import { Search, X, TrendingUp } from "lucide-react"
import { cn } from "@/lib/utils"

interface EnhancedSearchInputProps {
  value: string
  onChange: (value: string) => void
  onSubmit: (query: string) => void
  placeholder?: string
  className?: string
  disabled?: boolean
  autoFocus?: boolean
}

export function EnhancedSearchInput({
  value,
  onChange,
  onSubmit,
  placeholder = "Suchen...",
  className,
  disabled = false,
  autoFocus = false,
}: EnhancedSearchInputProps) {
  const [isFocused, setIsFocused] = useState(false)
  const [showSuggestions, setShowSuggestions] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  const { suggestions, isLoading, addSearch, clearHistory, removeSearch, getPopularKeys, stats } = useSearchSuggestions(
    value,
    {
      maxSuggestions: 8,
      debounceMs: 150,
      minQueryLength: 0,
    },
  )

  // Click outside to close suggestions
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setShowSuggestions(false)
      }
    }

    document.addEventListener("mousedown", handleClickOutside)
    return () => document.removeEventListener("mousedown", handleClickOutside)
  }, [])

  // Auto-focus
  useEffect(() => {
    if (autoFocus && inputRef.current) {
      inputRef.current.focus()
    }
  }, [autoFocus])

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value
    onChange(newValue)

    // Show suggestions when typing or when focused with empty input
    setShowSuggestions(true)
  }

  const handleInputFocus = () => {
    setIsFocused(true)
    setShowSuggestions(true)
  }

  const handleInputBlur = () => {
    setIsFocused(false)
    // Delay hiding suggestions to allow for clicks
    setTimeout(() => {
      if (!containerRef.current?.contains(document.activeElement)) {
        setShowSuggestions(false)
      }
    }, 150)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (value.trim()) {
      // Add to search cache
      addSearch(value.trim())
      onSubmit(value.trim())
      setShowSuggestions(false)
      inputRef.current?.blur()
    }
  }

  const handleSuggestionSelect = (query: string) => {
    onChange(query)
    addSearch(query)
    onSubmit(query)
    setShowSuggestions(false)
    inputRef.current?.blur()
  }

  const handleClearInput = () => {
    onChange("")
    inputRef.current?.focus()
    setShowSuggestions(true)
  }

  const handleClearHistory = () => {
    clearHistory()
    setShowSuggestions(false)
  }

  const handleRemoveSuggestion = (query: string) => {
    removeSearch(query)
  }

  // Smart placeholder based on popular keys
  const getSmartPlaceholder = () => {
    if (value) return placeholder

    const popularKeys = getPopularKeys()
    if (popularKeys.length > 0) {
      const randomKey = popularKeys[Math.floor(Math.random() * Math.min(3, popularKeys.length))]
      return `z.B. ${randomKey}:wert oder ${placeholder}`
    }

    return placeholder
  }

  return (
    <div ref={containerRef} className="relative w-full">
      <form onSubmit={handleSubmit} className="relative">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />

          <Input
            ref={inputRef}
            type="text"
            value={value}
            onChange={handleInputChange}
            onFocus={handleInputFocus}
            onBlur={handleInputBlur}
            placeholder={getSmartPlaceholder()}
            disabled={disabled}
            className={cn(
              "pl-10 pr-20 transition-all duration-200",
              isFocused && "ring-2 ring-emerald-500/20 border-emerald-300",
              showSuggestions && "rounded-b-none",
              className,
            )}
            autoComplete="off"
            spellCheck="false"
          />

          <div className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center gap-1">
            {/* Popular indicator */}
            {stats.totalSearches > 0 && (
              <div className="flex items-center gap-1 px-2 py-1 bg-gray-100 rounded text-xs text-gray-600">
                <TrendingUp className="h-3 w-3" />
                <span>{stats.totalSearches}</span>
              </div>
            )}

            {/* Clear button */}
            {value && (
              <Button
                type="button"
                onClick={handleClearInput}
                variant="ghost"
                size="sm"
                className="h-6 w-6 p-0 hover:bg-gray-100"
              >
                <X className="h-3 w-3" />
              </Button>
            )}

            {/* Submit button */}
            <Button
              type="submit"
              disabled={!value.trim() || disabled}
              variant="ghost"
              size="sm"
              className="h-6 w-6 p-0 hover:bg-emerald-100 disabled:opacity-50"
            >
              <Search className="h-3 w-3" />
            </Button>
          </div>
        </div>
      </form>

      {/* Suggestions Dropdown */}
      <SearchSuggestions
        suggestions={suggestions}
        isVisible={showSuggestions && (suggestions.length > 0 || isLoading)}
        onSelect={handleSuggestionSelect}
        onRemove={handleRemoveSuggestion}
        onClearAll={handleClearHistory}
        currentQuery={value}
        isLoading={isLoading}
        maxHeight="400px"
      />
    </div>
  )
}
