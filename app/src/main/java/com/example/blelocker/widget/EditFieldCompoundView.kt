package com.example.blelocker.widget


import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.getStringOrThrow
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.blelocker.R
import com.example.blelocker.databinding.EditTextCompoundBinding
import com.example.blelocker.gone
import com.example.blelocker.invisible
import com.example.blelocker.visible

class EditFieldCompoundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    private val _validInput = MutableLiveData<Boolean>()
    val validInput: LiveData<Boolean>
        get() = _validInput
    //binding
    private var binding: EditTextCompoundBinding =
        EditTextCompoundBinding.inflate(LayoutInflater.from(context), this)


    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.edit_field_compound,
            0,
            0
        ).apply {
            try {

                binding.tvTitle.text = this.getStringOrThrow(R.styleable.edit_field_compound_title)
                when (this.getInt(R.styleable.edit_field_compound_inputType, 0)) {
                    //code
                    2 -> {
                        binding.etContent.filters = arrayOf(InputFilter.LengthFilter(8))
                        binding.etContent.inputType = InputType.TYPE_CLASS_NUMBER
                        binding.etContent.transformationMethod = ReallyHideMyPassword()
                    }
                    //name
                    3 -> binding.etContent.inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
                    //email
                    4 -> binding.etContent.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    //password
                    5 -> {
                        binding.etContent.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                        binding.etContent.transformationMethod = ReallyHideMyPassword()
                    }
                    //text
                    else -> binding.etContent.inputType = InputType.TYPE_CLASS_TEXT
                }
                this.getResourceId(R.styleable.edit_field_compound_editTextStrokeColor, 0).let {
                    if (it != 0) {
                        binding.etContent.setBackgroundResource(it)
                    }
                }
            } catch (exception: RuntimeException) {
                Log.d("TAG",exception.toString())
            } finally {
                this.recycle()
            }
        }
        when (binding.etContent.inputType) {
            InputType.TYPE_CLASS_NUMBER -> {
                binding.etContent.addTextChangedListener {
                    val isValid = it?.toString()?.filter { digit -> digit.isDigit() }?.count() in 4..8
                    _validInput.value = isValid
                    toggleErrorCodeState(isValid)
                }
            }
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> {
                binding.etContent.addTextChangedListener {
                    val isValid = it?.let { editable ->
                        editable.toString().toByteArray().size in 1..20
                    } ?: false
                    _validInput.value = isValid
                    toggleErrorTextState(isValid)
                }
            }
            InputType.TYPE_CLASS_TEXT -> {
                binding.etContent.addTextChangedListener {
                    val isValid = it.toString().isNotBlank()
                    toggleErrorNameState(isValid)
                }
            }
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> {
                binding.etContent.addTextChangedListener {
                    val isValid = it.toString().isNotBlank()
                    toggleErrorEmailState(isValid)
                }
            }
            InputType.TYPE_TEXT_VARIATION_PASSWORD -> {
                binding.etContent.addTextChangedListener {
                    val isValid = it.toString().isNotBlank()
                    toggleErrorPWState(isValid)
                }
            }
        }
        binding.ivError.invisible()

    }

    fun getText(): String {
        return binding.etContent.text.toString()
    }

    fun setText(text: String) {
        if (text == context.getString(R.string.add_lock_admin_code_setup_already)) {
            binding.etContent.filters = arrayOf()
        }

        binding.etContent.setText(text, TextView.BufferType.EDITABLE)
    }

    private fun toggleErrorTextState(isValid: Boolean) {
        if (!isValid) {
            binding.ivError.visible()
            binding.tvError.visible()
            binding.tvError.text = context.getText(R.string.lock_name_exceeds)
            binding.etContent.setBackgroundResource(R.drawable.outline_edit_error)
        } else {
            binding.tvError.text = ""
            binding.ivError.invisible()
            binding.tvError.invisible()
            binding.etContent.setBackgroundResource(R.drawable.outline_edit)
        }
    }

    private fun toggleErrorNameState(isValid: Boolean) {
        if (!isValid) {
            binding.ivError.visible()
            binding.tvError.visible()
            binding.tvError.text = context.getText(R.string.setting_error_name_blank)
            binding.etContent.setBackgroundResource(R.drawable.outline_edit_error)
        } else {
            binding.tvError.text = ""
            binding.ivError.invisible()
            binding.tvError.invisible()
            binding.etContent.setBackgroundResource(R.drawable.outline_edit_disabled_variant)
        }
    }

    private fun toggleErrorPWState(isValid: Boolean) {
        if (!isValid) {
            binding.ivError.visible()
            binding.tvError.visible()
            binding.tvError.text = context.getText(R.string.setting_error_password_blank)
            binding.etContent.setBackgroundResource(R.drawable.outline_edit_error)
        } else {
            binding.tvError.text = ""
            binding.ivError.invisible()
            binding.tvError.invisible()
            binding.etContent.setBackgroundResource(R.drawable.outline_edit_disabled_variant)
        }
    }

    private fun toggleErrorEmailState(isValid: Boolean) {
        if (!isValid) {
            binding.ivError.visible()
            binding.tvError.visible()
            binding.tvError.text = context.getText(R.string.setting_error_email_blank)
            binding.etContent.setBackgroundResource(R.drawable.outline_edit_error)
        } else {
            binding.tvError.text = ""
            binding.ivError.invisible()
            binding.tvError.invisible()
            binding.etContent.setBackgroundResource(R.drawable.outline_edit_disabled_variant)
        }
    }

    private fun toggleErrorCodeState(isValid: Boolean) {
        if (!isValid) {
            binding.ivError.visible()
            binding.tvError.visible()
            binding.tvError.text = context.getText(R.string.global_pin_code_length)
            binding.etContent.setBackgroundResource(R.drawable.outline_edit_error)
        } else {
            binding.tvError.text = ""
            binding.ivError.invisible()
            binding.tvError.invisible()
            binding.etContent.setBackgroundResource(R.drawable.outline_edit)
        }
    }

    fun toggleEnableState(isEnabled: Boolean) {
        binding.etContent.isEnabled = isEnabled
        Log.d("TAG","et_content.inputType: ${binding.etContent.inputType}")
        if (!isEnabled) {
            binding.etContent.setTextColor(context.getColor(R.color.disconnected))
        }

        when (binding.etContent.inputType) {
            InputType.TYPE_CLASS_NUMBER -> {
                toggleEnableCodeViewStyle(isEnabled)
            }
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> {
                toggleEnableViewStyle(isEnabled)
            }
        }
    }

    private fun toggleEnableViewStyle(isEnabled: Boolean) {
        if (isEnabled) {
            binding.tvTitle.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.primary
                )
            )
            binding.etContent.setBackgroundResource(R.drawable.outline_edit)
//            et_content.inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            binding.ivError.invisible()
            binding.tvError.invisible()
        } else {
            binding.tvTitle.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.disconnected
                )
            )
//            et_content.inputType = InputType.TYPE_NULL
            binding.etContent.setBackgroundResource(R.drawable.outline_edit_disabled_variant)
            binding.ivError.invisible()
            binding.tvError.invisible()
        }
    }

    private fun toggleEnableCodeViewStyle(isEnabled: Boolean) {
        if (isEnabled) {
            binding.tvTitle.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.primary
                )
            )
            binding.etContent.setBackgroundResource(R.drawable.outline_edit)
            binding.ivError.invisible()
            binding.tvError.invisible()
        } else {
            binding.tvTitle.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.disconnected
                )
            )
            binding.etContent.setBackgroundResource(R.drawable.outline_edit_disabled_variant)
            binding.ivError.invisible()
            binding.tvError.invisible()
        }
    }

    fun setError(errorText: String?) {
        errorText?.let {
            binding.tvErrorBottom.text = errorText
            binding.ivErrorBottom.visible()
            binding.tvErrorBottom.visible()
        } ?: kotlin.run {
            binding.tvErrorBottom.text = ""
            binding.ivErrorBottom.gone()
            binding.tvErrorBottom.gone()
        }
    }
}
class ReallyHideMyPassword : PasswordTransformationMethod() {

    companion object {
        const val HIDE_CHAR = '●'
    }

    override fun getTransformation(source: CharSequence, view: View): CharSequence {
        return PasswordCharSequence(source)
    }

    inner class PasswordCharSequence (private val source: CharSequence) : CharSequence {

        override val length: Int
            get() = source.length

        override fun get(index: Int): Char = HIDE_CHAR

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            return source.subSequence(startIndex, endIndex)
        }
    }


}