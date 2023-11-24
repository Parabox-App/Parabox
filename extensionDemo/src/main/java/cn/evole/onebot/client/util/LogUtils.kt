package cn.evole.onebot.client.util

import android.util.Log
import androidx.compose.material.icons.materialIcon
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.Marker
import org.apache.logging.log4j.message.EntryMessage
import org.apache.logging.log4j.message.Message
import org.apache.logging.log4j.message.MessageFactory
import org.apache.logging.log4j.util.MessageSupplier
import org.apache.logging.log4j.util.Supplier

class LogUtils: Logger {
    override fun catching(level: Level?, throwable: Throwable?) {
        Log.d("demo", throwable.toString());
    }

    override fun catching(throwable: Throwable?) {
        Log.d("demo", throwable.toString());
    }

    override fun debug(marker: Marker?, message: Message?) {
        Log.d("demo", message.toString());
    }

    override fun debug(marker: Marker?, message: Message?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun debug(marker: Marker?, messageSupplier: MessageSupplier?) {
        Log.d("demo", marker.toString());
    }

    override fun debug(marker: Marker?, messageSupplier: MessageSupplier?, throwable: Throwable?) {
        Log.d("demo", throwable.toString());
    }

    override fun debug(marker: Marker?, message: CharSequence?) {
        Log.d("demo", message.toString());
    }

    override fun debug(marker: Marker?, message: CharSequence?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun debug(marker: Marker?, message: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(marker: Marker?, message: Any?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun debug(marker: Marker?, message: String?) {
        Log.d("demo", message.toString());
    }

    override fun debug(marker: Marker?, message: String?, vararg params: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(marker: Marker?, message: String?, vararg paramSuppliers: Supplier<*>?) {
        Log.d("demo", message.toString());
    }

    override fun debug(marker: Marker?, message: String?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun debug(marker: Marker?, messageSupplier: Supplier<*>?) {
        Log.d("demo", marker.toString());
    }

    override fun debug(marker: Marker?, messageSupplier: Supplier<*>?, throwable: Throwable?) {
        Log.d("demo", marker.toString());
    }

    override fun debug(message: Message?) {
        Log.d("demo", message.toString());
    }

    override fun debug(message: Message?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun debug(messageSupplier: MessageSupplier?) {
        Log.d("demo", messageSupplier.toString());
    }

    override fun debug(messageSupplier: MessageSupplier?, throwable: Throwable?) {
        Log.d("demo", messageSupplier.toString());
    }

    override fun debug(message: CharSequence?) {
        Log.d("demo", message.toString());
    }

    override fun debug(message: CharSequence?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun debug(message: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(message: Any?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun debug(message: String?) {
        Log.d("demo", message.toString());
    }

    override fun debug(message: String?, vararg params: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(message: String?, vararg paramSuppliers: Supplier<*>?) {
        Log.d("demo", message.toString());
    }

    override fun debug(message: String?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun debug(messageSupplier: Supplier<*>?) {
        Log.d("demo", messageSupplier.toString());
    }

    override fun debug(messageSupplier: Supplier<*>?, throwable: Throwable?) {
        Log.d("demo", messageSupplier.toString());
    }

    override fun debug(marker: Marker?, message: String?, p0: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(marker: Marker?, message: String?, p0: Any?, p1: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun debug(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun debug(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun debug(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun debug(message: String?, p0: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(message: String?, p0: Any?, p1: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(message: String?, p0: Any?, p1: Any?, p2: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?) {
        Log.d("demo", message.toString());
    }

    override fun debug(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun debug(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun debug(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun entry() {
    }

    override fun entry(vararg params: Any?) {
    }

    override fun error(marker: Marker?, message: Message?) {
        Log.d("demo", message.toString());
    }

    override fun error(marker: Marker?, message: Message?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun error(marker: Marker?, messageSupplier: MessageSupplier?) {
    }

    override fun error(marker: Marker?, messageSupplier: MessageSupplier?, throwable: Throwable?) {
    }

    override fun error(marker: Marker?, message: CharSequence?) {
        Log.d("demo", message.toString());
    }

    override fun error(marker: Marker?, message: CharSequence?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun error(marker: Marker?, message: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(marker: Marker?, message: Any?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun error(marker: Marker?, message: String?) {
        Log.d("demo", message.toString());
    }

    override fun error(marker: Marker?, message: String?, vararg params: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(marker: Marker?, message: String?, vararg paramSuppliers: Supplier<*>?) {
        Log.d("demo", message.toString());
    }

    override fun error(marker: Marker?, message: String?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun error(marker: Marker?, messageSupplier: Supplier<*>?) {
    }

    override fun error(marker: Marker?, messageSupplier: Supplier<*>?, throwable: Throwable?) {
    }

    override fun error(message: Message?) {
        Log.d("demo", message.toString());
    }

    override fun error(message: Message?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun error(messageSupplier: MessageSupplier?) {
    }

    override fun error(messageSupplier: MessageSupplier?, throwable: Throwable?) {
    }

    override fun error(message: CharSequence?) {
        Log.d("demo", message.toString());
    }

    override fun error(message: CharSequence?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun error(message: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(message: Any?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun error(message: String?) {
        Log.d("demo", message.toString());
    }

    override fun error(message: String?, vararg params: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(message: String?, vararg paramSuppliers: Supplier<*>?) {
        Log.d("demo", message.toString());
    }

    override fun error(message: String?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun error(messageSupplier: Supplier<*>?) {
    }

    override fun error(messageSupplier: Supplier<*>?, throwable: Throwable?) {
    }

    override fun error(marker: Marker?, message: String?, p0: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(marker: Marker?, message: String?, p0: Any?, p1: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun error(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun error(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun error(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun error(message: String?, p0: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(message: String?, p0: Any?, p1: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(message: String?, p0: Any?, p1: Any?, p2: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?) {
        Log.d("demo", message.toString());
    }

    override fun error(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun error(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun error(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun exit() {
    }

    override fun <R : Any?> exit(result: R): R {
        TODO()
    }

    override fun fatal(marker: Marker?, message: Message?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(marker: Marker?, message: Message?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(marker: Marker?, messageSupplier: MessageSupplier?) {
    }

    override fun fatal(marker: Marker?, messageSupplier: MessageSupplier?, throwable: Throwable?) {
    }

    override fun fatal(marker: Marker?, message: CharSequence?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(marker: Marker?, message: CharSequence?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(marker: Marker?, message: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(marker: Marker?, message: Any?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(marker: Marker?, message: String?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(marker: Marker?, message: String?, vararg params: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(marker: Marker?, message: String?, vararg paramSuppliers: Supplier<*>?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(marker: Marker?, message: String?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(marker: Marker?, messageSupplier: Supplier<*>?) {
    }

    override fun fatal(marker: Marker?, messageSupplier: Supplier<*>?, throwable: Throwable?) {
    }

    override fun fatal(message: Message?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(message: Message?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(messageSupplier: MessageSupplier?) {
    }

    override fun fatal(messageSupplier: MessageSupplier?, throwable: Throwable?) {
    }

    override fun fatal(message: CharSequence?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(message: CharSequence?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(message: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(message: Any?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(message: String?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(message: String?, vararg params: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(message: String?, vararg paramSuppliers: Supplier<*>?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(message: String?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(messageSupplier: Supplier<*>?) {
    }

    override fun fatal(messageSupplier: Supplier<*>?, throwable: Throwable?) {
    }

    override fun fatal(marker: Marker?, message: String?, p0: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(marker: Marker?, message: String?, p0: Any?, p1: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun fatal(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun fatal(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun fatal(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun fatal(message: String?, p0: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(message: String?, p0: Any?, p1: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(message: String?, p0: Any?, p1: Any?, p2: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?) {
        Log.d("demo", message.toString());
    }

    override fun fatal(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun fatal(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun fatal(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun getLevel(): Level {
        TODO()
    }

    override fun <MF : MessageFactory?> getMessageFactory(): MF {
        TODO()
    }

    override fun getName(): String {
        TODO()
    }

    override fun info(marker: Marker?, message: Message?) {
        Log.d("demo", message.toString());
    }

    override fun info(marker: Marker?, message: Message?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun info(marker: Marker?, messageSupplier: MessageSupplier?) {
    }

    override fun info(marker: Marker?, messageSupplier: MessageSupplier?, throwable: Throwable?) {
    }

    override fun info(marker: Marker?, message: CharSequence?) {
        Log.d("demo", message.toString());
    }

    override fun info(marker: Marker?, message: CharSequence?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun info(marker: Marker?, message: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(marker: Marker?, message: Any?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun info(marker: Marker?, message: String?) {
        Log.d("demo", message.toString());
    }

    override fun info(marker: Marker?, message: String?, vararg params: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(marker: Marker?, message: String?, vararg paramSuppliers: Supplier<*>?) {
        Log.d("demo", message.toString());
    }

    override fun info(marker: Marker?, message: String?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun info(marker: Marker?, messageSupplier: Supplier<*>?) {
    }

    override fun info(marker: Marker?, messageSupplier: Supplier<*>?, throwable: Throwable?) {
    }

    override fun info(message: Message?) {
        Log.d("demo", message.toString());
    }

    override fun info(message: Message?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun info(messageSupplier: MessageSupplier?) {
    }

    override fun info(messageSupplier: MessageSupplier?, throwable: Throwable?) {
    }

    override fun info(message: CharSequence?) {
        Log.d("demo", message.toString());
    }

    override fun info(message: CharSequence?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun info(message: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(message: Any?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun info(message: String?) {
        Log.d("demo", message.toString());
    }

    override fun info(message: String?, vararg params: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(message: String?, vararg paramSuppliers: Supplier<*>?) {
        Log.d("demo", message.toString());
    }

    override fun info(message: String?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun info(messageSupplier: Supplier<*>?) {
    }

    override fun info(messageSupplier: Supplier<*>?, throwable: Throwable?) {
    }

    override fun info(marker: Marker?, message: String?, p0: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(marker: Marker?, message: String?, p0: Any?, p1: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun info(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun info(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun info(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun info(message: String?, p0: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(message: String?, p0: Any?, p1: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(message: String?, p0: Any?, p1: Any?, p2: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?) {
        Log.d("demo", message.toString());
    }

    override fun info(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun info(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun info(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun isDebugEnabled(): Boolean {
        return true
    }

    override fun isDebugEnabled(marker: Marker?): Boolean {
        return true

    }

    override fun isEnabled(level: Level?): Boolean {
        return true
    }

    override fun isEnabled(level: Level?, marker: Marker?): Boolean {
        return true
    }

    override fun isErrorEnabled(): Boolean {
        return true
    }

    override fun isErrorEnabled(marker: Marker?): Boolean {
        return true
    }

    override fun isFatalEnabled(): Boolean {
        return true
    }

    override fun isFatalEnabled(marker: Marker?): Boolean {
        return true
    }

    override fun isInfoEnabled(): Boolean {
        return true
    }

    override fun isInfoEnabled(marker: Marker?): Boolean {
        return true
    }

    override fun isTraceEnabled(): Boolean {
        return true
    }

    override fun isTraceEnabled(marker: Marker?): Boolean {
        return true
    }

    override fun isWarnEnabled(): Boolean {
        return true
    }

    override fun isWarnEnabled(marker: Marker?): Boolean {
        return true
    }

    override fun log(level: Level?, marker: Marker?, message: Message?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, marker: Marker?, message: Message?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, marker: Marker?, messageSupplier: MessageSupplier?) {
    }

    override fun log(level: Level?, marker: Marker?, messageSupplier: MessageSupplier?, throwable: Throwable?) {
    }

    override fun log(level: Level?, marker: Marker?, message: CharSequence?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, marker: Marker?, message: CharSequence?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, marker: Marker?, message: Any?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, marker: Marker?, message: Any?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, marker: Marker?, message: String?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, marker: Marker?, message: String?, vararg params: Any?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, marker: Marker?, message: String?, vararg paramSuppliers: Supplier<*>?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, marker: Marker?, message: String?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, marker: Marker?, messageSupplier: Supplier<*>?) {
    }

    override fun log(level: Level?, marker: Marker?, messageSupplier: Supplier<*>?, throwable: Throwable?) {
    }

    override fun log(level: Level?, message: Message?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, message: Message?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, messageSupplier: MessageSupplier?) {
    }

    override fun log(level: Level?, messageSupplier: MessageSupplier?, throwable: Throwable?) {
    }

    override fun log(level: Level?, message: CharSequence?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, message: CharSequence?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, message: Any?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, message: Any?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, message: String?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, message: String?, vararg params: Any?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, message: String?, vararg paramSuppliers: Supplier<*>?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, message: String?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, messageSupplier: Supplier<*>?) {
    }

    override fun log(level: Level?, messageSupplier: Supplier<*>?, throwable: Throwable?) {
    }

    override fun log(level: Level?, marker: Marker?, message: String?, p0: Any?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, marker: Marker?, message: String?, p0: Any?, p1: Any?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?) {
        Log.d("demo", message.toString());
    }

    override fun log(
        level: Level?,
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun log(
        level: Level?,
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun log(
        level: Level?,
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun log(
        level: Level?,
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun log(
        level: Level?,
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun log(
        level: Level?,
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, message: String?, p0: Any?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, message: String?, p0: Any?, p1: Any?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, message: String?, p0: Any?, p1: Any?, p2: Any?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?) {
        Log.d("demo", message.toString());
    }

    override fun log(level: Level?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?) {
        Log.d("demo", message.toString());
    }

    override fun log(
        level: Level?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun log(
        level: Level?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun log(
        level: Level?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun log(
        level: Level?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun printf(level: Level?, marker: Marker?, format: String?, vararg params: Any?) {
        TODO()
    }

    override fun printf(level: Level?, format: String?, vararg params: Any?) {
    }

    override fun <T : Throwable?> throwing(level: Level?, throwable: T): T {
        TODO()
    }

    override fun <T : Throwable?> throwing(throwable: T): T {
        TODO()

    }

    override fun trace(marker: Marker?, message: Message?) {
        Log.d("demo", message.toString());
    }

    override fun trace(marker: Marker?, message: Message?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun trace(marker: Marker?, messageSupplier: MessageSupplier?) {
    }

    override fun trace(marker: Marker?, messageSupplier: MessageSupplier?, throwable: Throwable?) {
    }

    override fun trace(marker: Marker?, message: CharSequence?) {
        Log.d("demo", message.toString());
    }

    override fun trace(marker: Marker?, message: CharSequence?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun trace(marker: Marker?, message: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(marker: Marker?, message: Any?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun trace(marker: Marker?, message: String?) {
        Log.d("demo", message.toString());
    }

    override fun trace(marker: Marker?, message: String?, vararg params: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(marker: Marker?, message: String?, vararg paramSuppliers: Supplier<*>?) {
        Log.d("demo", message.toString());
    }

    override fun trace(marker: Marker?, message: String?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun trace(marker: Marker?, messageSupplier: Supplier<*>?) {
    }

    override fun trace(marker: Marker?, messageSupplier: Supplier<*>?, throwable: Throwable?) {
    }

    override fun trace(message: Message?) {
        Log.d("demo", message.toString());
    }

    override fun trace(message: Message?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun trace(messageSupplier: MessageSupplier?) {
    }

    override fun trace(messageSupplier: MessageSupplier?, throwable: Throwable?) {
    }

    override fun trace(message: CharSequence?) {
        Log.d("demo", message.toString());
    }

    override fun trace(message: CharSequence?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun trace(message: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(message: Any?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun trace(message: String?) {
        Log.d("demo", message.toString());
    }

    override fun trace(message: String?, vararg params: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(message: String?, vararg paramSuppliers: Supplier<*>?) {
        Log.d("demo", message.toString());
    }

    override fun trace(message: String?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun trace(messageSupplier: Supplier<*>?) {
    }

    override fun trace(messageSupplier: Supplier<*>?, throwable: Throwable?) {
    }

    override fun trace(marker: Marker?, message: String?, p0: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(marker: Marker?, message: String?, p0: Any?, p1: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun trace(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun trace(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun trace(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun trace(message: String?, p0: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(message: String?, p0: Any?, p1: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(message: String?, p0: Any?, p1: Any?, p2: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?) {
        Log.d("demo", message.toString());
    }

    override fun trace(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun trace(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun trace(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun traceEntry(): EntryMessage {
        TODO()
    }

    override fun traceEntry(format: String?, vararg params: Any?): EntryMessage {
        TODO()
    }

    override fun traceEntry(vararg paramSuppliers: Supplier<*>?): EntryMessage {
        TODO()

    }

    override fun traceEntry(format: String?, vararg paramSuppliers: Supplier<*>?): EntryMessage {
        TODO()

    }

    override fun traceEntry(message: Message?): EntryMessage {
        Log.d("demo", message.toString());
        TODO()
    }

    override fun traceExit() {
    }

    override fun <R : Any?> traceExit(result: R): R {
        TODO()
    }

    override fun <R : Any?> traceExit(format: String?, result: R): R {
        TODO()

    }

    override fun traceExit(message: EntryMessage?) {
        Log.d("demo", message.toString());
    }

    override fun <R : Any?> traceExit(message: EntryMessage?, result: R): R {
        Log.d("demo", message.toString());
        TODO()

    }

    override fun <R : Any?> traceExit(message: Message?, result: R): R {
        Log.d("demo", message.toString());
        TODO()

    }

    override fun warn(marker: Marker?, message: Message?) {
        Log.d("demo", message.toString());
    }

    override fun warn(marker: Marker?, message: Message?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun warn(marker: Marker?, messageSupplier: MessageSupplier?) {
        TODO()
    }

    override fun warn(marker: Marker?, messageSupplier: MessageSupplier?, throwable: Throwable?) {
    }

    override fun warn(marker: Marker?, message: CharSequence?) {
        Log.d("demo", message.toString());
    }

    override fun warn(marker: Marker?, message: CharSequence?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun warn(marker: Marker?, message: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(marker: Marker?, message: Any?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun warn(marker: Marker?, message: String?) {
        Log.d("demo", message.toString());
    }

    override fun warn(marker: Marker?, message: String?, vararg params: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(marker: Marker?, message: String?, vararg paramSuppliers: Supplier<*>?) {
        Log.d("demo", message.toString());
    }

    override fun warn(marker: Marker?, message: String?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun warn(marker: Marker?, messageSupplier: Supplier<*>?) {
    }

    override fun warn(marker: Marker?, messageSupplier: Supplier<*>?, throwable: Throwable?) {
    }

    override fun warn(message: Message?) {
        Log.d("demo", message.toString());
    }

    override fun warn(message: Message?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun warn(messageSupplier: MessageSupplier?) {
    }

    override fun warn(messageSupplier: MessageSupplier?, throwable: Throwable?) {
    }

    override fun warn(message: CharSequence?) {
        Log.d("demo", message.toString());
    }

    override fun warn(message: CharSequence?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun warn(message: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(message: Any?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun warn(message: String?) {
        Log.d("demo", message.toString());
    }

    override fun warn(message: String?, vararg params: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(message: String?, vararg paramSuppliers: Supplier<*>?) {
        Log.d("demo", message.toString());
    }

    override fun warn(message: String?, throwable: Throwable?) {
        Log.d("demo", message.toString());
    }

    override fun warn(messageSupplier: Supplier<*>?) {
    }

    override fun warn(messageSupplier: Supplier<*>?, throwable: Throwable?) {
    }

    override fun warn(marker: Marker?, message: String?, p0: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(marker: Marker?, message: String?, p0: Any?, p1: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(marker: Marker?, message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun warn(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun warn(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun warn(
        marker: Marker?,
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun warn(message: String?, p0: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(message: String?, p0: Any?, p1: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(message: String?, p0: Any?, p1: Any?, p2: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(message: String?, p0: Any?, p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?) {
        Log.d("demo", message.toString());
    }

    override fun warn(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun warn(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?
    ) {
        Log.d("demo", message.toString());
    }

    override fun warn(
        message: String?,
        p0: Any?,
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?
    ) {
        Log.d("demo", message.toString());
    }

}